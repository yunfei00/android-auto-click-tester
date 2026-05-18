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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yunfei.autoclicktester.engine.AutoClickEngine
import com.yunfei.autoclicktester.service.AutoClickAccessibilityService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AutoClickScreen() } }
    }
}

@Composable
fun AutoClickScreen() {
    val context = LocalContext.current
    val status = remember { mutableStateOf("Idle") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Android Auto Click Tester")
        Text("Status: ${status.value}")

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
            if (service == null) {
                status.value = "请先启用无障碍服务"
            } else {
                AutoClickEngine.start(service)
                status.value = "Clicking at 1 tap/sec"
            }
        }) { Text("开始点击") }

        Button(onClick = {
            AutoClickEngine.stop()
            status.value = "Stopped"
        }) { Text("停止点击") }
    }
}
