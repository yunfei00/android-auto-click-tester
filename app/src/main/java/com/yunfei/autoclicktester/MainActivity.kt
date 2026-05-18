package com.yunfei.autoclicktester

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yunfei.autoclicktester.engine.AutoClickEngine
import com.yunfei.autoclicktester.model.ClickConfig
import com.yunfei.autoclicktester.service.AutoClickAccessibilityService
import com.yunfei.autoclicktester.storage.ClickConfigStorage
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AutoClickScreen() } }
    }
}

@Composable
fun AutoClickScreen() {
    val context = LocalContext.current
    val initialConfig = remember { ClickConfigStorage.get(context) }
    val status = remember { mutableStateOf("Idle") }
    val intervalText = remember { mutableStateOf(initialConfig.intervalMs.toString()) }
    val xPercentText = remember { mutableStateOf(trimPercent(initialConfig.xPercent)) }
    val yPercentText = remember { mutableStateOf(trimPercent(initialConfig.yPercent)) }
    val showTouchMarker = remember { mutableStateOf(initialConfig.showTouchMarker) }
    val clickCount = remember { mutableStateOf(0L) }
    val lastClickTime = remember { mutableStateOf("尚未点击") }
    val timeFormatter = remember { DateFormat.getTimeInstance(DateFormat.MEDIUM) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Android Auto Click Tester")
        Text("Status: ${status.value}")
        Text("点击次数：${clickCount.value}")
        Text("最后一次：${lastClickTime.value}")

        OutlinedTextField(
            value = intervalText.value,
            onValueChange = { intervalText.value = it },
            label = { Text("点击间隔 (ms)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = xPercentText.value,
            onValueChange = { xPercentText.value = it },
            label = { Text("X 位置 (%)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = yPercentText.value,
            onValueChange = { yPercentText.value = it },
            label = { Text("Y 位置 (%)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = showTouchMarker.value,
                onCheckedChange = { showTouchMarker.value = it }
            )
            Text("显示点击标记")
        }

        Button(onClick = {
            val config = parseClickConfig(
                intervalText.value,
                xPercentText.value,
                yPercentText.value,
                showTouchMarker.value
            )
            if (config == null) {
                status.value = "配置无效：间隔 100-60000，位置 0-100"
            } else {
                ClickConfigStorage.save(context, config)
                status.value = "配置已保存"
            }
        }) { Text("保存配置") }

        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }) { Text("启动无障碍服务") }

        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                status.value = "Overlay permission required"
            } else {
                status.value = "Overlay permission granted"
            }
        }) { Text("悬浮窗权限检测") }

        Button(onClick = {
            val service = AutoClickAccessibilityService.instance
            val config = parseClickConfig(
                intervalText.value,
                xPercentText.value,
                yPercentText.value,
                showTouchMarker.value
            )
            if (config == null) {
                status.value = "配置无效：间隔 100-60000，位置 0-100"
            } else if (service == null) {
                status.value = "请先启用无障碍服务"
            } else {
                ClickConfigStorage.save(context, config)
                clickCount.value = 0L
                lastClickTime.value = "等待第一次点击"
                AutoClickEngine.start(service, config) { count ->
                    clickCount.value = count
                    lastClickTime.value = timeFormatter.format(Date())
                    status.value = "点击中：第 ${count} 次 @ ${trimPercent(config.xPercent)}%, ${trimPercent(config.yPercent)}%"
                }
                status.value = if (config.showTouchMarker && !canDrawOverlay(context)) {
                    "点击中：未授权悬浮窗，标记不可见"
                } else {
                    "点击中：等待第一次点击"
                }
            }
        }) { Text("开始点击") }

        Button(onClick = {
            AutoClickEngine.stop()
            status.value = "Stopped"
        }) { Text("停止点击") }
    }
}

private fun parseClickConfig(
    intervalText: String,
    xPercentText: String,
    yPercentText: String,
    showTouchMarker: Boolean
): ClickConfig? {
    val intervalMs = intervalText.trim().toLongOrNull() ?: return null
    val xPercent = xPercentText.trim().toFloatOrNull() ?: return null
    val yPercent = yPercentText.trim().toFloatOrNull() ?: return null

    if (intervalMs !in 100L..60_000L) return null
    if (xPercent !in 0f..100f) return null
    if (yPercent !in 0f..100f) return null

    return ClickConfig(
        intervalMs = intervalMs,
        xPercent = xPercent,
        yPercent = yPercent,
        showTouchMarker = showTouchMarker
    )
}

private fun trimPercent(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}

private fun canDrawOverlay(context: android.content.Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
}
