package com.yunfei.autoclicktester.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.roundToInt

object PointPickerOverlay {
    private var pickerView: View? = null

    fun show(
        context: Context,
        onPicked: (xPercent: Float, yPercent: Float) -> Unit
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return false
        val windowManager = context.getSystemService(WindowManager::class.java) ?: return false
        hide(context)

        val view = View(context).apply {
            setBackgroundColor(Color.argb(35, 0, 0, 0))
            setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val width = v.width.coerceAtLeast(1)
                    val height = v.height.coerceAtLeast(1)
                    val xPercent = (event.x / width * 100f).coerceIn(0f, 100f)
                    val yPercent = (event.y / height * 100f).coerceIn(0f, 100f)
                    onPicked(xPercent, yPercent)
                    hide(context)
                    true
                } else {
                    false
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0.roundToInt()
            y = 0.roundToInt()
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
            // ignore
        } finally {
            pickerView = null
        }
    }
}
