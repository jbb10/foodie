package com.foodie.app.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility to centralize notification permission checks.
 */
object NotificationPermissionManager {
    const val PERMISSION = Manifest.permission.POST_NOTIFICATIONS

    fun isPermissionRequired(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun hasPermission(context: Context): Boolean {
        if (!isPermissionRequired()) return true
        return ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED
    }
}
