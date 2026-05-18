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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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

        Button(onClick = {
            val config = parseClickConfig(intervalText.value, xPercentText.value, yPercentText.value)
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
            val config = parseClickConfig(intervalText.value, xPercentText.value, yPercentText.value)
            if (config == null) {
                status.value = "配置无效：间隔 100-60000，位置 0-100"
            } else if (service == null) {
                status.value = "请先启用无障碍服务"
            } else {
                ClickConfigStorage.save(context, config)
                AutoClickEngine.start(service, config)
                status.value = "点击中：${config.intervalMs}ms @ ${trimPercent(config.xPercent)}%, ${trimPercent(config.yPercent)}%"
            }
        }) { Text("开始点击") }

        Button(onClick = {
            AutoClickEngine.stop()
            status.value = "Stopped"
        }) { Text("停止点击") }
    }
}

private fun parseClickConfig(intervalText: String, xPercentText: String, yPercentText: String): ClickConfig? {
    val intervalMs = intervalText.trim().toLongOrNull() ?: return null
    val xPercent = xPercentText.trim().toFloatOrNull() ?: return null
    val yPercent = yPercentText.trim().toFloatOrNull() ?: return null

    if (intervalMs !in 100L..60_000L) return null
    if (xPercent !in 0f..100f) return null
    if (yPercent !in 0f..100f) return null

    return ClickConfig(
        intervalMs = intervalMs,
        xPercent = xPercent,
        yPercent = yPercent
    )
}

private fun trimPercent(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}
