package com.yunfei.autoclicktester.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.roundToInt

object PointPickerOverlay {
    private var pickerView: View? = null

    fun show(
        context: Context,
        label: String,
        onPicked: (xPercent: Float, yPercent: Float) -> Unit
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return false
        val windowManager = context.getSystemService(WindowManager::class.java) ?: return false
        hide(context)

        val view = PointPickerView(context.applicationContext, label).apply {
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val metrics = readScreenMetrics(windowManager)
                    val xPercent = ((event.rawX - metrics.offsetX) / metrics.width * 100f).coerceIn(0f, 100f)
                    val yPercent = ((event.rawY - metrics.offsetY) / metrics.height * 100f).coerceIn(0f, 100f)
                    onPicked(xPercent, yPercent)
                    hide(context)
                    true
                } else {
                    true
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        return try {
            windowManager.addView(view, params)
            pickerView = view
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    fun hide(context: Context) {
        val view = pickerView ?: return
        val windowManager = context.getSystemService(WindowManager::class.java) ?: return
        try {
            if (view.isAttachedToWindow) {
                windowManager.removeView(view)
            }
        } catch (_: RuntimeException) {
            // The overlay can already be detached if the app moves between windows.
        } finally {
            pickerView = null
        }
    }

    private fun readScreenMetrics(windowManager: WindowManager): ScreenMetrics {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            ScreenMetrics(
                width = bounds.width().coerceAtLeast(1).toFloat(),
                height = bounds.height().coerceAtLeast(1).toFloat(),
                offsetX = bounds.left.toFloat(),
                offsetY = bounds.top.toFloat()
            )
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            ScreenMetrics(
                width = metrics.widthPixels.coerceAtLeast(1).toFloat(),
                height = metrics.heightPixels.coerceAtLeast(1).toFloat(),
                offsetX = 0f,
                offsetY = 0f
            )
        }
    }

    private data class ScreenMetrics(
        val width: Float,
        val height: Float,
        val offsetX: Float,
        val offsetY: Float
    )
}

private class PointPickerView(context: Context, private val label: String) : View(context) {
    private val density = resources.displayMetrics.density
    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(46, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (label == "2") Color.rgb(0, 150, 255) else Color.rgb(255, 48, 48)
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 20, 20, 20)
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 15f * resources.displayMetrics.scaledDensity
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = 38f * density
        canvas.drawCircle(centerX, centerY, radius, ringPaint)
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, ringPaint)
        canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, ringPaint)

        val badgeWidth = 108f * density
        val badgeHeight = 32f * density
        val badgeLeft = centerX - badgeWidth / 2f
        val badgeTop = (24f * density).roundToInt().toFloat()
        canvas.drawRoundRect(
            badgeLeft,
            badgeTop,
            badgeLeft + badgeWidth,
            badgeTop + badgeHeight,
            10f * density,
            10f * density,
            badgePaint
        )
        val textY = badgeTop + badgeHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("选择点 $label", centerX, textY, textPaint)
    }
}
