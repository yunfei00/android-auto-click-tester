package com.yunfei.autoclicktester.engine

import android.os.Handler
import android.os.Looper
import com.yunfei.autoclicktester.service.AutoClickAccessibilityService

object AutoClickEngine {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val clickTask = object : Runnable {
        override fun run() {
            if (!running) return
            service?.clickCenter()
            handler.postDelayed(this, 1000L)
        }
    }

    private var service: AutoClickAccessibilityService? = null

    fun start(accessibilityService: AutoClickAccessibilityService) {
        if (running) return
        service = accessibilityService
        running = true
        handler.post(clickTask)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(clickTask)
        service = null
    }
}
