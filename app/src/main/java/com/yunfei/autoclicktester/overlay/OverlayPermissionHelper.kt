package com.yunfei.autoclicktester.overlay

import android.content.Context
import android.os.Build
import android.provider.Settings

object OverlayPermissionHelper {
    fun hasOverlayPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }
}
