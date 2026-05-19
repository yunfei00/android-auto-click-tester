package com.yunfei.autoclicktester.engine

import android.os.Handler
import android.os.Looper
import com.yunfei.autoclicktester.model.ClickConfig
import com.yunfei.autoclicktester.model.ClickPointConfig
import com.yunfei.autoclicktester.service.AutoClickAccessibilityService

data class ClickProgress(
    val pointLabel: String,
    val pointCount: Long,
    val totalCount: Long,
    val xPercent: Float,
    val yPercent: Float
)

object AutoClickEngine {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var config = ClickConfig()
    private var totalClickCount = 0L
    private val pointClickCounts = mutableMapOf<String, Long>()
    private val clickTasks = mutableListOf<Runnable>()
    private var progressCallback: ((ClickProgress) -> Unit)? = null
    private var service: AutoClickAccessibilityService? = null

    fun start(
        accessibilityService: AutoClickAccessibilityService,
        clickConfig: ClickConfig,
        onProgress: (ClickProgress) -> Unit
    ) {
        stop()
        service = accessibilityService
        config = clickConfig
        progressCallback = onProgress
        totalClickCount = 0L
        pointClickCounts.clear()
        config.activePoints.forEach { pointClickCounts[it.label] = 0L }
        running = true

        config.activePoints.forEachIndexed { index, point ->
            val task = createClickTask(point)
            clickTasks += task
            handler.postDelayed(task, index * POINT_START_OFFSET_MS)
        }
    }

    fun stop() {
        running = false
        clickTasks.forEach(handler::removeCallbacks)
        clickTasks.clear()
        progressCallback = null
        service = null
    }

    private fun createClickTask(point: ClickPointConfig): Runnable {
        return object : Runnable {
            override fun run() {
                if (!running) return
                val accepted = service?.clickAt(
                    xPercent = point.xPercent,
                    yPercent = point.yPercent,
                    showTouchMarker = config.showTouchMarker,
                    markerLabel = point.label
                ) == true
                if (accepted) {
                    val pointCount = (pointClickCounts[point.label] ?: 0L) + 1L
                    pointClickCounts[point.label] = pointCount
                    totalClickCount += 1L
                    progressCallback?.invoke(
                        ClickProgress(
                            pointLabel = point.label,
                            pointCount = pointCount,
                            totalCount = totalClickCount,
                            xPercent = point.xPercent,
                            yPercent = point.yPercent
                        )
                    )
                }
                handler.postDelayed(this, point.intervalMs)
            }
        }
    }

    private const val POINT_START_OFFSET_MS = 160L
}
