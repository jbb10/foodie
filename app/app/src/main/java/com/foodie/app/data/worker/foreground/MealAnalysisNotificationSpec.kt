package com.foodie.app.data.worker.foreground

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Shared configuration for meal analysis foreground notifications.
 */
object MealAnalysisNotificationSpec {
    const val CHANNEL_ID = "meal_analysis_silent"
    const val CHANNEL_NAME = "Meal Analysis"
    const val CHANNEL_DESCRIPTION = "Silent notifications for background meal logging."
    const val ONGOING_NOTIFICATION_ID = 201
    const val COMPLETION_NOTIFICATION_ID = 202  // Different ID for success/failure notifications

    data class NotificationChannelSpec(
        val id: String,
        val name: String,
        val description: String,
        val importance: Int,
        val showBadge: Boolean,
        val enableVibration: Boolean,
        val enableSound: Boolean
    )

    fun channelSpec(): NotificationChannelSpec = NotificationChannelSpec(
        id = CHANNEL_ID,
        name = CHANNEL_NAME,
        description = CHANNEL_DESCRIPTION,
        importance = NotificationManager.IMPORTANCE_LOW,  // Silent notifications
        showBadge = false,
        enableVibration = false,
        enableSound = false
    )

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val spec = channelSpec()

        val existing = manager.getNotificationChannel(spec.id)
        val channel = NotificationChannel(spec.id, spec.name, spec.importance).apply {
            description = spec.description
            setShowBadge(spec.showBadge)
            enableVibration(spec.enableVibration)
            if (!spec.enableSound) {
                setSound(null, null)
            }
        }

        if (existing == null) {
            manager.createNotificationChannel(channel)
        } else {
            val needsUpdate = existing.importance != spec.importance ||
                existing.description != spec.description ||
                existing.canShowBadge() != spec.showBadge ||
                existing.shouldVibrate() != spec.enableVibration

            if (needsUpdate) {
                manager.deleteNotificationChannel(spec.id)
                manager.createNotificationChannel(channel)
            }
        }
    }
}
