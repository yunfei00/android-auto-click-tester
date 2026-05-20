package com.yunfei.autoclicktester.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.database.ContentObserver
import android.graphics.Path
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.provider.Settings
import com.yunfei.autoclicktester.engine.AutoClickEngine
import com.yunfei.autoclicktester.engine.StopReason
import com.yunfei.autoclicktester.overlay.ClickMarkerOverlay

class AutoClickAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile
        var instance: AutoClickAccessibilityService? = null
            private set

        private val VOLUME_STREAMS = intArrayOf(
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_ALARM
        )
    }

    private var consumeVolumeDownKey = false
    private var lastStreamVolumes = emptyMap<Int, Int>()
    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            handleVolumeChange()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        info.notificationTimeout = 100
        setServiceInfo(info)
        instance = this
        lastStreamVolumes = readStreamVolumes()
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        AutoClickEngine.stop(StopReason.ServiceInterrupted)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.action == KeyEvent.ACTION_DOWN && AutoClickEngine.isRunning) {
                consumeVolumeDownKey = true
                AutoClickEngine.stop(StopReason.VolumeDown)
                return true
            }
            if (event.action == KeyEvent.ACTION_UP && consumeVolumeDownKey) {
                consumeVolumeDownKey = false
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onDestroy() {
        AutoClickEngine.stop(StopReason.ServiceDestroyed)
        ClickMarkerOverlay.hide(this)
        try {
            contentResolver.unregisterContentObserver(volumeObserver)
        } catch (_: RuntimeException) {
            // The observer can already be unregistered when the service is torn down.
        }
        super.onDestroy()
        instance = null
    }

    private fun handleVolumeChange() {
        val currentVolumes = readStreamVolumes()
        val volumeWentDown = currentVolumes.any { (streamType, volume) ->
            val previousVolume = lastStreamVolumes[streamType]
            previousVolume != null && volume < previousVolume
        }
        lastStreamVolumes = currentVolumes
        if (volumeWentDown && AutoClickEngine.isRunning) {
            AutoClickEngine.stop(StopReason.VolumeDown)
        }
    }

    private fun readStreamVolumes(): Map<Int, Int> {
        val audioManager = getSystemService(AudioManager::class.java) ?: return emptyMap()
        return VOLUME_STREAMS.associateWith { streamType ->
            audioManager.getStreamVolume(streamType)
        }
    }

    fun clickAt(
        xPercent: Float,
        yPercent: Float,
        showTouchMarker: Boolean,
        markerLabel: String
    ): Boolean {
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
            ClickMarkerOverlay.flashAt(this, centerX, centerY, markerLabel)
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
