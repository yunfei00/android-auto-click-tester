package com.yunfei.autoclicktester

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yunfei.autoclicktester.engine.AutoClickEngine
import com.yunfei.autoclicktester.model.ClickConfig
import com.yunfei.autoclicktester.model.ClickPointConfig
import com.yunfei.autoclicktester.overlay.OverlayPermissionHelper
import com.yunfei.autoclicktester.overlay.PointPickerOverlay
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
    val status = remember { mutableStateOf("未开始") }
    val statusTone = remember { mutableStateOf(StatusTone.Info) }
    val pointOneIntervalText = remember { mutableStateOf(initialConfig.pointOne.intervalMs.toString()) }
    val pointOneXText = remember { mutableStateOf(trimPercent(initialConfig.pointOne.xPercent)) }
    val pointOneYText = remember { mutableStateOf(trimPercent(initialConfig.pointOne.yPercent)) }
    val pointTwoIntervalText = remember { mutableStateOf(initialConfig.pointTwo.intervalMs.toString()) }
    val pointTwoXText = remember { mutableStateOf(trimPercent(initialConfig.pointTwo.xPercent)) }
    val pointTwoYText = remember { mutableStateOf(trimPercent(initialConfig.pointTwo.yPercent)) }
    val enableSecondPoint = remember { mutableStateOf(initialConfig.enableSecondPoint) }
    val showTouchMarker = remember { mutableStateOf(initialConfig.showTouchMarker) }
    val totalClickCount = remember { mutableStateOf(0L) }
    val pointOneClickCount = remember { mutableStateOf(0L) }
    val pointTwoClickCount = remember { mutableStateOf(0L) }
    val lastClickTime = remember { mutableStateOf("尚未点击") }
    val timeFormatter = remember { DateFormat.getTimeInstance(DateFormat.MEDIUM) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Android Auto Click Tester",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        StatusBanner(status.value, statusTone.value)
        Text("总点击次数：${totalClickCount.value}")
        Text(
            if (enableSecondPoint.value) {
                "点 1：${pointOneClickCount.value} 次    点 2：${pointTwoClickCount.value} 次"
            } else {
                "点 1：${pointOneClickCount.value} 次"
            }
        )
        Text("最后一次：${lastClickTime.value}")

        PointConfigFields(
            title = "点 1",
            intervalText = pointOneIntervalText.value,
            xPercentText = pointOneXText.value,
            yPercentText = pointOneYText.value,
            onIntervalChange = { pointOneIntervalText.value = it },
            onXPercentChange = { pointOneXText.value = it },
            onYPercentChange = { pointOneYText.value = it }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (!OverlayPermissionHelper.hasOverlayPermission(context)) {
                        showStatus(status, statusTone, StatusTone.Warning, "请先开启悬浮窗权限后再选点")
                    } else {
                        val shown = PointPickerOverlay.show(context) { x, y ->
                            pointOneXText.value = trimPercent(x)
                            pointOneYText.value = trimPercent(y)
                        }
                        if (shown) showStatus(status, statusTone, StatusTone.Info, "请点击屏幕选择点 1")
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("选择点1") }
            Button(
                onClick = {
                    if (!OverlayPermissionHelper.hasOverlayPermission(context)) {
                        showStatus(status, statusTone, StatusTone.Warning, "请先开启悬浮窗权限后再选点")
                    } else {
                        val shown = PointPickerOverlay.show(context) { x, y ->
                            pointTwoXText.value = trimPercent(x)
                            pointTwoYText.value = trimPercent(y)
                        }
                        if (shown) showStatus(status, statusTone, StatusTone.Info, "请点击屏幕选择点 2")
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("选择点2") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = enableSecondPoint.value,
                onCheckedChange = { enableSecondPoint.value = it }
            )
            Text("启用第二个点")
        }

        if (enableSecondPoint.value) {
            PointConfigFields(
                title = "点 2",
                intervalText = pointTwoIntervalText.value,
                xPercentText = pointTwoXText.value,
                yPercentText = pointTwoYText.value,
                onIntervalChange = { pointTwoIntervalText.value = it },
                onXPercentChange = { pointTwoXText.value = it },
                onYPercentChange = { pointTwoYText.value = it }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = showTouchMarker.value,
                onCheckedChange = { showTouchMarker.value = it }
            )
            Text("显示点击标记（红色 1 / 蓝色 2）")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            val config = parseClickConfig(
                pointOneIntervalText.value,
                pointOneXText.value,
                pointOneYText.value,
                pointTwoIntervalText.value,
                pointTwoXText.value,
                pointTwoYText.value,
                enableSecondPoint.value,
                showTouchMarker.value
            )
            if (config == null) {
                showStatus(status, statusTone, StatusTone.Error, "配置无效：间隔 100-60000，位置 0-100")
            } else {
                ClickConfigStorage.save(context, config)
                showStatus(status, statusTone, StatusTone.Success, "配置已保存")
            }
        }) { Text("保存配置") }

        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            showStatus(status, statusTone, StatusTone.Warning, "请在系统设置中开启 Auto Click Tester 无障碍服务")
        }) { Text("启动无障碍服务") }

        Button(onClick = {
            if (!OverlayPermissionHelper.hasOverlayPermission(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                showStatus(status, statusTone, StatusTone.Warning, "请开启悬浮窗权限，否则看不到点击标记")
            } else {
                showStatus(status, statusTone, StatusTone.Success, "悬浮窗权限已开启")
            }
        }) { Text("悬浮窗权限检测") }

        Button(onClick = {
            val service = AutoClickAccessibilityService.instance
            val config = parseClickConfig(
                pointOneIntervalText.value,
                pointOneXText.value,
                pointOneYText.value,
                pointTwoIntervalText.value,
                pointTwoXText.value,
                pointTwoYText.value,
                enableSecondPoint.value,
                showTouchMarker.value
            )
            if (config == null) {
                showStatus(status, statusTone, StatusTone.Error, "配置无效：间隔 100-60000，位置 0-100")
            } else if (service == null) {
                showStatus(
                    status,
                    statusTone,
                    StatusTone.Error,
                    "请先启用无障碍服务：点击“启动无障碍服务”，在系统设置中开启 Auto Click Tester"
                )
            } else if (config.showTouchMarker && !OverlayPermissionHelper.hasOverlayPermission(context)) {
                showStatus(status, statusTone, StatusTone.Error, "请先开启悬浮窗权限，否则看不到点击标记")
            } else {
                ClickConfigStorage.save(context, config)
                totalClickCount.value = 0L
                pointOneClickCount.value = 0L
                pointTwoClickCount.value = 0L
                lastClickTime.value = "等待第一次点击"
                AutoClickEngine.start(service, config) { progress ->
                    totalClickCount.value = progress.totalCount
                    if (progress.pointLabel == "2") {
                        pointTwoClickCount.value = progress.pointCount
                    } else {
                        pointOneClickCount.value = progress.pointCount
                    }
                    lastClickTime.value = "点 ${progress.pointLabel} · ${timeFormatter.format(Date())}"
                    showStatus(
                        status,
                        statusTone,
                        StatusTone.Running,
                        "点击中：点 ${progress.pointLabel} 第 ${progress.pointCount} 次 @ ${trimPercent(progress.xPercent)}%, ${trimPercent(progress.yPercent)}%"
                    )
                }
                showStatus(status, statusTone, StatusTone.Running, "点击中：等待第一次点击")
            }
        }, modifier = Modifier.weight(1f)) { Text("开始点击") }

        Button(onClick = {
            AutoClickEngine.stop()
            showStatus(status, statusTone, StatusTone.Info, "已停止")
        }, modifier = Modifier.weight(1f)) { Text("停止点击") }
        }
    }
}

@Composable
private fun StatusBanner(message: String, tone: StatusTone) {
    val colors = MaterialTheme.colorScheme
    val containerColor = when (tone) {
        StatusTone.Error -> colors.errorContainer
        StatusTone.Warning -> colors.secondaryContainer
        StatusTone.Success -> colors.primaryContainer
        StatusTone.Running -> colors.tertiaryContainer
        StatusTone.Info -> colors.surfaceVariant
    }
    val contentColor = when (tone) {
        StatusTone.Error -> colors.onErrorContainer
        StatusTone.Warning -> colors.onSecondaryContainer
        StatusTone.Success -> colors.onPrimaryContainer
        StatusTone.Running -> colors.onTertiaryContainer
        StatusTone.Info -> colors.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp
    ) {
        Text(
            text = "Status: $message",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (tone == StatusTone.Error) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun PointConfigFields(
    title: String,
    intervalText: String,
    xPercentText: String,
    yPercentText: String,
    onIntervalChange: (String) -> Unit,
    onXPercentChange: (String) -> Unit,
    onYPercentChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = intervalText,
            onValueChange = onIntervalChange,
            label = { Text("$title 间隔 (ms)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = xPercentText,
            onValueChange = onXPercentChange,
            label = { Text("$title X 位置 (%)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = yPercentText,
            onValueChange = onYPercentChange,
            label = { Text("$title Y 位置 (%)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun parseClickConfig(
    pointOneIntervalText: String,
    pointOneXText: String,
    pointOneYText: String,
    pointTwoIntervalText: String,
    pointTwoXText: String,
    pointTwoYText: String,
    enableSecondPoint: Boolean,
    showTouchMarker: Boolean
): ClickConfig? {
    val pointOne = parsePointConfig("1", pointOneIntervalText, pointOneXText, pointOneYText) ?: return null
    val pointTwo = parsePointConfig("2", pointTwoIntervalText, pointTwoXText, pointTwoYText) ?: return null
    return ClickConfig(
        pointOne = pointOne,
        pointTwo = pointTwo,
        enableSecondPoint = enableSecondPoint,
        showTouchMarker = showTouchMarker
    )
}

private fun parsePointConfig(
    label: String,
    intervalText: String,
    xPercentText: String,
    yPercentText: String
): ClickPointConfig? {
    val intervalMs = intervalText.trim().toLongOrNull() ?: return null
    val xPercent = xPercentText.trim().toFloatOrNull() ?: return null
    val yPercent = yPercentText.trim().toFloatOrNull() ?: return null

    if (intervalMs !in 100L..60_000L) return null
    if (xPercent !in 0f..100f) return null
    if (yPercent !in 0f..100f) return null

    return ClickPointConfig(
        label = label,
        intervalMs = intervalMs,
        xPercent = xPercent,
        yPercent = yPercent
    )
}

private fun showStatus(
    status: androidx.compose.runtime.MutableState<String>,
    statusTone: androidx.compose.runtime.MutableState<StatusTone>,
    tone: StatusTone,
    message: String
) {
    status.value = message
    statusTone.value = tone
}

private fun trimPercent(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}

private enum class StatusTone {
    Info,
    Success,
    Warning,
    Error,
    Running
}
