package com.yunfei.autoclicktester.engine

import android.os.Handler
import android.os.Looper
import com.yunfei.autoclicktester.model.ClickConfig
import com.yunfei.autoclicktester.service.AutoClickAccessibilityService

object AutoClickEngine {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var config = ClickConfig()
    private var clickCount = 0L
    private var progressCallback: ((Long) -> Unit)? = null

    private val clickTask = object : Runnable {
        override fun run() {
            if (!running) return
            val accepted = service?.clickAt(
                xPercent = config.xPercent,
                yPercent = config.yPercent,
                showTouchMarker = config.showTouchMarker
            ) == true
            if (accepted) {
                clickCount += 1
                progressCallback?.invoke(clickCount)
            }
            handler.postDelayed(this, config.intervalMs)
        }
    }

    private var service: AutoClickAccessibilityService? = null

    fun start(
        accessibilityService: AutoClickAccessibilityService,
        clickConfig: ClickConfig,
        onProgress: (Long) -> Unit
    ) {
        service = accessibilityService
        config = clickConfig
        progressCallback = onProgress
        if (running) return
        clickCount = 0L
        running = true
        handler.post(clickTask)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(clickTask)
        progressCallback = null
        service = null
    }
}
