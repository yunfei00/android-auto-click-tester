package com.yunfei.autoclicktester.engine

import android.os.Handler
import android.os.Looper
import com.yunfei.autoclicktester.model.ClickConfig
import com.yunfei.autoclicktester.service.AutoClickAccessibilityService

object AutoClickEngine {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var config = ClickConfig()

    private val clickTask = object : Runnable {
        override fun run() {
            if (!running) return
            service?.clickAt(config.xPercent, config.yPercent)
            handler.postDelayed(this, config.intervalMs)
        }
    }

    private var service: AutoClickAccessibilityService? = null

    fun start(accessibilityService: AutoClickAccessibilityService, clickConfig: ClickConfig) {
        service = accessibilityService
        config = clickConfig
        if (running) return
        running = true
        handler.post(clickTask)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(clickTask)
        service = null
    }
}
