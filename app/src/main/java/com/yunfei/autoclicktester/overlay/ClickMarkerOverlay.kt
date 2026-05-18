package com.yunfei.autoclicktester.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlin.math.roundToInt

object ClickMarkerOverlay {
    private const val MARKER_SIZE_DP = 56
    private const val MARKER_DURATION_MS = 220L

    private val handler = Handler(Looper.getMainLooper())
    private var markerView: ClickMarkerView? = null
    private var hideRunnable: Runnable? = null

    fun flashAt(context: Context, x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        handler.post {
            val windowManager = context.getSystemService(WindowManager::class.java) ?: return@post
            val density = context.resources.displayMetrics.density
            val markerSize = (MARKER_SIZE_DP * density).roundToInt()
            val view = markerView ?: ClickMarkerView(context.applicationContext).also { markerView = it }
            val params = WindowManager.LayoutParams(
                markerSize,
                markerSize,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = (x - markerSize / 2f).roundToInt()
                this.y = (y - markerSize / 2f).roundToInt()
            }

            try {
                if (view.isAttachedToWindow) {
                    windowManager.updateViewLayout(view, params)
                } else {
                    windowManager.addView(view, params)
                }
            } catch (_: RuntimeException) {
                return@post
            }

            view.invalidate()
            hideRunnable?.let(handler::removeCallbacks)
            hideRunnable = Runnable { hide(context) }.also {
                handler.postDelayed(it, MARKER_DURATION_MS)
            }
        }
    }

    fun hide(context: Context) {
        handler.post {
            val windowManager = context.getSystemService(WindowManager::class.java) ?: return@post
            val view = markerView ?: return@post
            if (!view.isAttachedToWindow) return@post
            try {
                windowManager.removeView(view)
            } catch (_: RuntimeException) {
                return@post
            }
        }
    }
}

private class ClickMarkerView(context: Context) : View(context) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(72, 255, 64, 64)
        style = Paint.Style.FILL
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 48, 48)
        style = Paint.Style.STROKE
        strokeWidth = 5f * resources.displayMetrics.density
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(width, height) * 0.36f
        canvas.drawCircle(centerX, centerY, radius, fillPaint)
        canvas.drawCircle(centerX, centerY, radius, ringPaint)
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, linePaint)
        canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, linePaint)
    }
}
