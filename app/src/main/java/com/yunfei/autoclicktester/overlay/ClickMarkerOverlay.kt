package com.yunfei.autoclicktester.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlin.math.roundToInt

object ClickMarkerOverlay {
    private const val MARKER_SIZE_DP = 82
    private const val MARKER_DURATION_MS = 700L

    private val handler = Handler(Looper.getMainLooper())
    private val markerViews = mutableMapOf<String, ClickMarkerView>()
    private val hideRunnables = mutableMapOf<String, Runnable>()

    fun flashAt(context: Context, x: Float, y: Float, label: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        handler.post {
            val windowManager = context.getSystemService(WindowManager::class.java) ?: return@post
            val density = context.resources.displayMetrics.density
            val markerSize = (MARKER_SIZE_DP * density).roundToInt()
            val view = markerViews.getOrPut(label) {
                ClickMarkerView(context.applicationContext)
            }
            view.setMarker(label, markerColor(label))
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
            hideRunnables.remove(label)?.let(handler::removeCallbacks)
            hideRunnables[label] = Runnable { hideLabel(context, label) }.also {
                handler.postDelayed(it, MARKER_DURATION_MS)
            }
        }
    }

    fun hide(context: Context) {
        handler.post {
            val windowManager = context.getSystemService(WindowManager::class.java) ?: return@post
            hideRunnables.values.forEach(handler::removeCallbacks)
            hideRunnables.clear()
            markerViews.values.forEach { view ->
                if (view.isAttachedToWindow) {
                    try {
                        windowManager.removeView(view)
                    } catch (_: RuntimeException) {
                        return@forEach
                    }
                }
            }
            markerViews.clear()
        }
    }

    private fun hideLabel(context: Context, label: String) {
        handler.post {
            val windowManager = context.getSystemService(WindowManager::class.java) ?: return@post
            hideRunnables.remove(label)
            val view = markerViews[label] ?: return@post
            if (!view.isAttachedToWindow) return@post
            try {
                windowManager.removeView(view)
            } catch (_: RuntimeException) {
                return@post
            }
        }
    }

    private fun markerColor(label: String): Int {
        return if (label == "2") Color.rgb(0, 150, 255) else Color.rgb(255, 48, 48)
    }
}

private class ClickMarkerView(context: Context) : View(context) {
    private var markerLabel = "1"
    private var markerColor = Color.rgb(255, 48, 48)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f * resources.displayMetrics.density
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 28f * resources.displayMetrics.scaledDensity
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f * resources.displayMetrics.density, 0f, 0f, Color.BLACK)
    }

    fun setMarker(label: String, color: Int) {
        markerLabel = label
        markerColor = color
        fillPaint.color = colorWithAlpha(color, 96)
        ringPaint.color = color
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
        val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(markerLabel, centerX, textY, textPaint)
    }

    private fun colorWithAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
