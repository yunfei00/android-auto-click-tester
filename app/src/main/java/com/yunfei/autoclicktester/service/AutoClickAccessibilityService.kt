package com.yunfei.autoclicktester.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.yunfei.autoclicktester.overlay.ClickMarkerOverlay

class AutoClickAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile
        var instance: AutoClickAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        ClickMarkerOverlay.hide(this)
        super.onDestroy()
        instance = null
    }

    fun clickAt(xPercent: Float, yPercent: Float, showTouchMarker: Boolean): Boolean {
        val windowManager = getSystemService(WindowManager::class.java) ?: return false
        val safeXPercent = xPercent.coerceIn(0f, 100f)
        val safeYPercent = yPercent.coerceIn(0f, 100f)

        val metrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            ScreenMetrics(
                bounds.width().toFloat(),
                bounds.height().toFloat(),
                bounds.left.toFloat(),
                bounds.top.toFloat()
            )
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            ScreenMetrics(metrics.widthPixels.toFloat(), metrics.heightPixels.toFloat(), 0f, 0f)
        }
        val centerX = metrics.offsetX + metrics.width * safeXPercent / 100f
        val centerY = metrics.offsetY + metrics.height * safeYPercent / 100f

        val path = Path().apply { moveTo(centerX, centerY) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 80L))
            .build()
        val accepted = dispatchGesture(gesture, null, null)
        if (accepted && showTouchMarker) {
            ClickMarkerOverlay.flashAt(this, centerX, centerY)
        }
        return accepted
    }

    private data class ScreenMetrics(
        val width: Float,
        val height: Float,
        val offsetX: Float,
        val offsetY: Float
    )
}
