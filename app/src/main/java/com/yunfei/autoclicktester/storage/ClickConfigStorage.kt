package com.yunfei.autoclicktester.storage

import android.content.Context
import com.yunfei.autoclicktester.model.ClickConfig

object ClickConfigStorage {
    private const val PREFS_NAME = "click_config"
    private const val KEY_INTERVAL_MS = "interval_ms"
    private const val KEY_X_PERCENT = "x_percent"
    private const val KEY_Y_PERCENT = "y_percent"
    private const val KEY_SHOW_TOUCH_MARKER = "show_touch_marker"

    fun get(context: Context): ClickConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ClickConfig(
            intervalMs = prefs.getLong(KEY_INTERVAL_MS, ClickConfig().intervalMs),
            xPercent = prefs.getFloat(KEY_X_PERCENT, ClickConfig().xPercent),
            yPercent = prefs.getFloat(KEY_Y_PERCENT, ClickConfig().yPercent),
            showTouchMarker = prefs.getBoolean(KEY_SHOW_TOUCH_MARKER, ClickConfig().showTouchMarker)
        )
    }

    fun save(context: Context, newConfig: ClickConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_INTERVAL_MS, newConfig.intervalMs)
            .putFloat(KEY_X_PERCENT, newConfig.xPercent)
            .putFloat(KEY_Y_PERCENT, newConfig.yPercent)
            .putBoolean(KEY_SHOW_TOUCH_MARKER, newConfig.showTouchMarker)
            .apply()
    }
}
