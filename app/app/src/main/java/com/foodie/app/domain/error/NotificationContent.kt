package com.foodie.app.domain.error

import android.app.PendingIntent

/**
 * Notification content for error conditions.
 *
 * Provides structured notification data for displaying error alerts to users.
 * Used by ErrorHandler.getNotificationContent() to generate notification
 * data for different error types.
 *
 * Architecture:
 * - ErrorHandler generates NotificationContent for each ErrorType
 * - NotificationManager wrapper consumes NotificationContent to display notifications
 * - Supports actionable notifications (Retry, Open Settings, etc.)
 *
 * Story: 4.1 - Network & Error Handling Infrastructure
 *
 * @property title Notification title (brief summary)
 * @property message Detailed user-friendly message explaining the error
 * @property actionText Optional action button text ("Retry", "Open Settings", etc.)
 * @property actionIntent Optional PendingIntent for action button
 * @property isOngoing True for persistent notifications requiring user action
 */
data class NotificationContent(
    val title: String,
    val message: String,
    val actionText: String? = null,
    val actionIntent: PendingIntent? = null,
    val isOngoing: Boolean = false,
) {
    companion object {
        /**
         * Creates notification content for network errors.
         *
         * Non-ongoing notification with no action (user should check network settings).
         *
         * Usage:
         * ```
         * val content = NotificationContent.networkError()
         * notificationManager.show(content)
         * ```
         */
        fun networkError(): NotificationContent = NotificationContent(
            title = "Network Error",
            message = "Request timed out. Check your internet connection.",
            actionText = null,
            actionIntent = null,
            isOngoing = false,
        )

        /**
         * Creates notification content for server errors.
         *
         * Non-ongoing notification indicating automatic retry is in progress.
         */
        fun serverError(): NotificationContent = NotificationContent(
            title = "Service Unavailable",
            message = "Service temporarily unavailable. Will retry automatically.",
            actionText = null,
            actionIntent = null,
            isOngoing = false,
        )

        /**
         * Creates notification content for authentication errors.
         *
         * Ongoing notification with action to open settings (requires user intervention).
         *
         * @param settingsIntent PendingIntent to open app settings screen
         */
        fun authError(settingsIntent: PendingIntent?): NotificationContent = NotificationContent(
            title = "Configuration Error",
            message = "API key invalid. Check settings.",
            actionText = "Open Settings",
            actionIntent = settingsIntent,
            isOngoing = true,
        )

        /**
         * Creates notification content for permission denied errors.
         *
         * Ongoing notification with action to grant permissions.
         *
         * @param permissionsIntent PendingIntent to open app permissions screen
         */
        fun permissionDenied(permissionsIntent: PendingIntent?): NotificationContent = NotificationContent(
            title = "Permissions Required",
            message = "Health Connect permissions required. Tap to grant access.",
            actionText = "Grant Access",
            actionIntent = permissionsIntent,
            isOngoing = true,
        )

        /**
         * Creates notification content for rate limit errors.
         *
         * Non-ongoing notification informing user to wait before retrying.
         */
        fun rateLimit(): NotificationContent = NotificationContent(
            title = "Too Many Requests",
            message = "Too many requests. Please wait a moment.",
            actionText = null,
            actionIntent = null,
            isOngoing = false,
        )
    }
}
