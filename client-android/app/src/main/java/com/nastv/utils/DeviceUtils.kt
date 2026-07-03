package com.nastv.utils

import android.content.Context
import android.content.pm.PackageManager

object DeviceUtils {

    /**
     * Check if the current device has a touchscreen.
     * Returns true for phones/tablets, false for TV devices.
     */
    fun hasTouchscreen(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    }

    /**
     * Check if the device is a TV (Android TV / Fire TV).
     */
    fun isTvDevice(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    /**
     * Check if the device supports picture-in-picture mode.
     */
    fun supportsPip(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
    }
}
