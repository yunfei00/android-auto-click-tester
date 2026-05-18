package com.yunfei.autoclicktester.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent

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
        super.onDestroy()
        instance = null
    }

    fun clickCenter() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val centerX = metrics.widthPixels / 2f
        val centerY = metrics.heightPixels / 2f

        val path = Path().apply { moveTo(centerX, centerY) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 80L))
            .build()
        dispatchGesture(gesture, null, null)
    }
}
