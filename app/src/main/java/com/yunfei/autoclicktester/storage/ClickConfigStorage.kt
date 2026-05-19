package com.yunfei.autoclicktester.storage

import android.content.Context
import com.yunfei.autoclicktester.model.ClickConfig
import com.yunfei.autoclicktester.model.ClickPointConfig

object ClickConfigStorage {
    private const val PREFS_NAME = "click_config"
    private const val KEY_INTERVAL_MS = "interval_ms"
    private const val KEY_X_PERCENT = "x_percent"
    private const val KEY_Y_PERCENT = "y_percent"
    private const val KEY_POINT_TWO_INTERVAL_MS = "point_two_interval_ms"
    private const val KEY_POINT_TWO_X_PERCENT = "point_two_x_percent"
    private const val KEY_POINT_TWO_Y_PERCENT = "point_two_y_percent"
    private const val KEY_ENABLE_SECOND_POINT = "enable_second_point"
    private const val KEY_SHOW_TOUCH_MARKER = "show_touch_marker"

    fun get(context: Context): ClickConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultConfig = ClickConfig()
        return ClickConfig(
            pointOne = ClickPointConfig(
                label = "1",
                intervalMs = prefs.getLong(KEY_INTERVAL_MS, defaultConfig.pointOne.intervalMs),
                xPercent = prefs.getFloat(KEY_X_PERCENT, defaultConfig.pointOne.xPercent),
                yPercent = prefs.getFloat(KEY_Y_PERCENT, defaultConfig.pointOne.yPercent)
            ),
            pointTwo = ClickPointConfig(
                label = "2",
                intervalMs = prefs.getLong(KEY_POINT_TWO_INTERVAL_MS, defaultConfig.pointTwo.intervalMs),
                xPercent = prefs.getFloat(KEY_POINT_TWO_X_PERCENT, defaultConfig.pointTwo.xPercent),
                yPercent = prefs.getFloat(KEY_POINT_TWO_Y_PERCENT, defaultConfig.pointTwo.yPercent)
            ),
            enableSecondPoint = prefs.getBoolean(
                KEY_ENABLE_SECOND_POINT,
                defaultConfig.enableSecondPoint
            ),
            showTouchMarker = prefs.getBoolean(KEY_SHOW_TOUCH_MARKER, defaultConfig.showTouchMarker)
        )
    }

    fun save(context: Context, newConfig: ClickConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_INTERVAL_MS, newConfig.pointOne.intervalMs)
            .putFloat(KEY_X_PERCENT, newConfig.pointOne.xPercent)
            .putFloat(KEY_Y_PERCENT, newConfig.pointOne.yPercent)
            .putLong(KEY_POINT_TWO_INTERVAL_MS, newConfig.pointTwo.intervalMs)
            .putFloat(KEY_POINT_TWO_X_PERCENT, newConfig.pointTwo.xPercent)
            .putFloat(KEY_POINT_TWO_Y_PERCENT, newConfig.pointTwo.yPercent)
            .putBoolean(KEY_ENABLE_SECOND_POINT, newConfig.enableSecondPoint)
            .putBoolean(KEY_SHOW_TOUCH_MARKER, newConfig.showTouchMarker)
            .apply()
    }
}
