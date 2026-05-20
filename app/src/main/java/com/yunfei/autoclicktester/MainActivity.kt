package com.yunfei.autoclicktester

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yunfei.autoclicktester.engine.AutoClickEngine
import com.yunfei.autoclicktester.engine.StopReason
import com.yunfei.autoclicktester.model.ClickConfig
import com.yunfei.autoclicktester.model.ClickPointConfig
import com.yunfei.autoclicktester.overlay.OverlayPermissionHelper
import com.yunfei.autoclicktester.overlay.PointPickerOverlay
import com.yunfei.autoclicktester.service.AutoClickAccessibilityService
import com.yunfei.autoclicktester.storage.ClickConfigStorage
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

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
    val isRunning = remember { mutableStateOf(AutoClickEngine.isRunning) }
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

    fun currentConfig(): ClickConfig? {
        return parseClickConfig(
            pointOneIntervalText.value,
            pointOneXText.value,
            pointOneYText.value,
            pointTwoIntervalText.value,
            pointTwoXText.value,
            pointTwoYText.value,
            enableSecondPoint.value,
            showTouchMarker.value
        )
    }

    fun pickPoint(label: String, onPicked: (Float, Float) -> Unit) {
        if (!OverlayPermissionHelper.hasOverlayPermission(context)) {
            openOverlayPermission(context)
            showStatus(status, statusTone, StatusTone.Warning, "请先开启悬浮窗权限")
            return
        }
        val shown = PointPickerOverlay.show(context, label) { xPercent, yPercent ->
            onPicked(xPercent, yPercent)
            showStatus(status, statusTone, StatusTone.Success, "点 $label 已选择：${trimPercent(xPercent)}%, ${trimPercent(yPercent)}%")
        }
        if (shown) {
            showStatus(status, statusTone, StatusTone.Info, "正在选择点 $label")
        } else {
            showStatus(status, statusTone, StatusTone.Error, "选点浮层启动失败")
        }
    }

    fun saveConfig() {
        val config = currentConfig()
        if (config == null) {
            showStatus(status, statusTone, StatusTone.Error, "配置无效：间隔 100-60000，位置 0-100")
            return
        }
        ClickConfigStorage.save(context, config)
        showStatus(status, statusTone, StatusTone.Success, "配置已保存")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Auto Click Tester",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "0.5.0",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        StatusBanner(status.value, statusTone.value)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatCell("总数", totalClickCount.value.toString(), Modifier.weight(1f))
            StatCell("点1", pointOneClickCount.value.toString(), Modifier.weight(1f))
            StatCell("点2", pointTwoClickCount.value.toString(), Modifier.weight(1f))
            StatCell("最后", lastClickTime.value, Modifier.weight(1.55f))
        }

        PointConfigRow(
            title = "点 1",
            intervalText = pointOneIntervalText.value,
            xPercentText = pointOneXText.value,
            yPercentText = pointOneYText.value,
            onIntervalChange = { pointOneIntervalText.value = it },
            onXPercentChange = { pointOneXText.value = it },
            onYPercentChange = { pointOneYText.value = it },
            onPickClick = {
                pickPoint("1") { xPercent, yPercent ->
                    pointOneXText.value = trimPercent(xPercent)
                    pointOneYText.value = trimPercent(yPercent)
                }
            }
        )

        PointConfigRow(
            title = "点 2",
            intervalText = pointTwoIntervalText.value,
            xPercentText = pointTwoXText.value,
            yPercentText = pointTwoYText.value,
            onIntervalChange = { pointTwoIntervalText.value = it },
            onXPercentChange = { pointTwoXText.value = it },
            onYPercentChange = { pointTwoYText.value = it },
            onPickClick = {
                pickPoint("2") { xPercent, yPercent ->
                    pointTwoXText.value = trimPercent(xPercent)
                    pointTwoYText.value = trimPercent(yPercent)
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactCheckbox(
                checked = enableSecondPoint.value,
                onCheckedChange = { enableSecondPoint.value = it },
                label = "启用点 2",
                modifier = Modifier.weight(1f)
            )
            CompactCheckbox(
                checked = showTouchMarker.value,
                onCheckedChange = { showTouchMarker.value = it },
                label = "显示标记",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactActionButton(
                text = "无障碍",
                onClick = {
                    openAccessibilitySettings(context)
                    showStatus(status, statusTone, StatusTone.Warning, "请开启 Auto Click Tester 无障碍服务")
                },
                modifier = Modifier.weight(1f)
            )
            CompactActionButton(
                text = "悬浮窗",
                onClick = {
                    if (OverlayPermissionHelper.hasOverlayPermission(context)) {
                        showStatus(status, statusTone, StatusTone.Success, "悬浮窗权限已开启")
                    } else {
                        openOverlayPermission(context)
                        showStatus(status, statusTone, StatusTone.Warning, "请开启悬浮窗权限")
                    }
                },
                modifier = Modifier.weight(1f)
            )
            CompactActionButton(
                text = "保存",
                onClick = { saveConfig() },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val service = AutoClickAccessibilityService.instance
                    val config = currentConfig()
                    when {
                        config == null -> {
                            showStatus(status, statusTone, StatusTone.Error, "配置无效：间隔 100-60000，位置 0-100")
                        }
                        service == null -> {
                            showStatus(status, statusTone, StatusTone.Error, "请先启用无障碍服务")
                        }
                        config.showTouchMarker && !OverlayPermissionHelper.hasOverlayPermission(context) -> {
                            showStatus(status, statusTone, StatusTone.Error, "请先开启悬浮窗权限")
                        }
                        else -> {
                            ClickConfigStorage.save(context, config)
                            totalClickCount.value = 0L
                            pointOneClickCount.value = 0L
                            pointTwoClickCount.value = 0L
                            lastClickTime.value = "等待首次点击"
                            isRunning.value = true
                            AutoClickEngine.start(
                                accessibilityService = service,
                                clickConfig = config,
                                onProgress = { progress ->
                                    totalClickCount.value = progress.totalCount
                                    if (progress.pointLabel == "2") {
                                        pointTwoClickCount.value = progress.pointCount
                                    } else {
                                        pointOneClickCount.value = progress.pointCount
                                    }
                                    lastClickTime.value = "点${progress.pointLabel} ${timeFormatter.format(Date())}"
                                    showStatus(
                                        status,
                                        statusTone,
                                        StatusTone.Running,
                                        "运行中：点${progress.pointLabel} 第 ${progress.pointCount} 次"
                                    )
                                },
                                onStopped = { reason ->
                                    isRunning.value = false
                                    showStatus(status, statusTone, StatusTone.Info, stopMessage(reason))
                                }
                            )
                            showStatus(status, statusTone, StatusTone.Running, "运行中")
                        }
                    }
                },
                enabled = !isRunning.value,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("开始点击", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Button(
                onClick = { AutoClickEngine.stop(StopReason.UserRequest) },
                enabled = isRunning.value,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("停止点击", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
        tonalElevation = 1.dp
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (tone == StatusTone.Error) FontWeight.Bold else FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(44.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PointConfigRow(
    title: String,
    intervalText: String,
    xPercentText: String,
    yPercentText: String,
    onIntervalChange: (String) -> Unit,
    onXPercentChange: (String) -> Unit,
    onYPercentChange: (String) -> Unit,
    onPickClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .width(38.dp)
                .padding(bottom = 7.dp),
            maxLines = 1
        )
        CompactNumberField(
            label = "间隔",
            value = intervalText,
            onValueChange = onIntervalChange,
            allowDecimal = false,
            modifier = Modifier.weight(1.15f)
        )
        CompactNumberField(
            label = "X%",
            value = xPercentText,
            onValueChange = onXPercentChange,
            allowDecimal = true,
            modifier = Modifier.weight(0.8f)
        )
        CompactNumberField(
            label = "Y%",
            value = yPercentText,
            onValueChange = onYPercentChange,
            allowDecimal = true,
            modifier = Modifier.weight(0.8f)
        )
        OutlinedButton(
            onClick = onPickClick,
            modifier = Modifier
                .height(36.dp)
                .widthIn(min = 58.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text("选点", maxLines = 1)
        }
    }
}

@Composable
private fun CompactNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    allowDecimal: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(sanitizeNumberInput(it, allowDecimal)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .border(BorderStroke(1.dp, colors.outline), MaterialTheme.shapes.extraSmall)
                .background(colors.surface, MaterialTheme.shapes.extraSmall)
                .padding(horizontal = 8.dp),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isBlank()) {
                        Text(
                            "--",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun CompactCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.width(34.dp)
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}

@Composable
private fun CompactActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    val pointTwo = parsePointConfig("2", pointTwoIntervalText, pointTwoXText, pointTwoYText)
        ?: run {
            if (enableSecondPoint) return null
            ClickConfig().pointTwo
        }
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
    status: MutableState<String>,
    statusTone: MutableState<StatusTone>,
    tone: StatusTone,
    message: String
) {
    status.value = message
    statusTone.value = tone
}

private fun trimPercent(value: Float): String {
    val rounded = value.roundToInt()
    return if (kotlin.math.abs(value - rounded) < 0.05f) {
        rounded.toString()
    } else {
        "%.1f".format(value)
    }
}

private fun sanitizeNumberInput(input: String, allowDecimal: Boolean): String {
    var hasDecimal = false
    return buildString {
        input.forEach { char ->
            when {
                char.isDigit() -> append(char)
                allowDecimal && char == '.' && !hasDecimal -> {
                    append(char)
                    hasDecimal = true
                }
            }
        }
    }.take(8)
}

private fun openAccessibilitySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun openOverlayPermission(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun stopMessage(reason: StopReason): String {
    return when (reason) {
        StopReason.UserRequest -> "已停止"
        StopReason.VolumeDown -> "已通过音量下键停止"
        StopReason.ServiceInterrupted -> "无障碍服务已中断"
        StopReason.ServiceDestroyed -> "无障碍服务已关闭"
    }
}

private enum class StatusTone {
    Info,
    Success,
    Warning,
    Error,
    Running
}
