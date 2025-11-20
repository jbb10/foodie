package com.foodie.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.foodie.app.MainActivity
import com.foodie.app.R
import com.foodie.app.domain.error.ErrorHandler
import com.foodie.app.domain.error.ErrorType
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for creating persistent error notifications with action buttons.
 *
 * Handles critical error notifications that require user intervention:
 * - Authentication errors (invalid API key) → "Open Settings" action
 * - Network errors (after retry exhaustion) → "Retry" action
 * - Permission errors → "Grant Access" action
 *
 * Separate from MealAnalysisForegroundNotifier which handles ongoing analysis notifications.
 * NotificationHelper creates persistent, actionable notifications for post-analysis failures.
 *
 * Architecture:
 * - Singleton: Injected via Hilt for consistent notification channel management
 * - ErrorHandler: Injected for consistent error messaging (single source of truth)
 * - PendingIntent: All action buttons use FLAG_IMMUTABLE for Android 12+ compatibility
 * - Deep Linking: Settings action uses intent action "com.foodie.app.OPEN_SETTINGS"
 * - Work Retry: Retry action re-enqueues AnalyzeMealWorker with same input data
 *
 * Story: 4.3 - API Error Classification and Handling
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorHandler: ErrorHandler
) {
    
    companion object {
        private const val TAG = "NotificationHelper"
        
        /**
         * Notification channel for error notifications.
         * Separate from "meal_analysis" channel for foreground service.
         */
        const val ERROR_CHANNEL_ID = "meal_analysis_errors"
        
        /**
         * Base notification ID for error notifications.
         * Different from foreground service IDs to avoid conflicts.
         */
        private const val ERROR_NOTIFICATION_ID = 2001
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Creates notification channel for error notifications.
     *
     * Called once during NotificationHelper initialization.
     * Channel settings:
     * - Importance: DEFAULT (shows notification, may make sound)
     * - Description: Alerts user to meal analysis errors requiring action
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ERROR_CHANNEL_ID,
            context.getString(R.string.notification_channel_errors_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_errors_description)
        }
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        
        Timber.tag(TAG).d("Error notification channel created: $ERROR_CHANNEL_ID")
    }
    
    /**
     * Shows error notification with appropriate action button.
     *
     * Notification actions by error type:
     * - AuthError → "Open Settings" to fix API key
     * - NetworkError (exhausted) → "Retry" to re-enqueue work
     * - PermissionDenied → "Grant Access" to Health Connect permissions
     * - Other errors → No action button (informational only)
     *
     * @param errorType The classified error type from ErrorHandler
     * @param workId Optional work ID for retry action (required for NetworkError retry)
     * @param photoUri Optional photo URI for retry action (required for NetworkError retry)
     * @param timestamp Optional timestamp for retry action (required for NetworkError retry)
     */
    fun showErrorNotification(
        errorType: ErrorType,
        workId: UUID? = null,
        photoUri: String? = null,
        timestamp: Long? = null
    ) {
        val title = getErrorTitle(errorType)
        val message = getErrorMessage(errorType)
        
        val builder = NotificationCompat.Builder(context, ERROR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createMainActivityIntent())
        
        // Add action button based on error type
        when (errorType) {
            is ErrorType.AuthError -> {
                builder.addAction(
                    R.drawable.ic_launcher_foreground,
                    context.getString(R.string.notification_action_open_settings),
                    createSettingsIntent()
                )
                Timber.tag(TAG).i("Persistent notification shown for AuthError with Settings action")
            }
            is ErrorType.NetworkError -> {
                // Only add retry action if we have work context
                if (workId != null && photoUri != null && timestamp != null) {
                    builder.addAction(
                        R.drawable.ic_launcher_foreground,
                        context.getString(R.string.notification_action_retry),
                        createRetryIntent(workId, photoUri, timestamp)
                    )
                    Timber.tag(TAG).i("Persistent notification shown for NetworkError with Retry action")
                } else {
                    Timber.tag(TAG).w("NetworkError notification without work context - no retry action")
                }
            }
            is ErrorType.PermissionDenied -> {
                builder.addAction(
                    R.drawable.ic_launcher_foreground,
                    context.getString(R.string.notification_action_grant_access),
                    createPermissionsIntent()
                )
                Timber.tag(TAG).i("Persistent notification shown for PermissionDenied with Grant Access action")
            }
            else -> {
                // No action button for other error types
                Timber.tag(TAG).i("Notification shown for ${errorType.javaClass.simpleName} (no action)")
            }
        }
        
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(ERROR_NOTIFICATION_ID, builder.build())
            Timber.tag(TAG).d("Error notification displayed: $title")
        } catch (e: SecurityException) {
            Timber.tag(TAG).e(e, "Failed to show notification - missing POST_NOTIFICATIONS permission")
        }
    }
    
    /**
     * Creates PendingIntent to launch Settings screen.
     *
     * Uses intent action "com.foodie.app.OPEN_SETTINGS" which MainActivity handles
     * in onCreate() to navigate to settings.
     *
     * @return PendingIntent for Settings action button
     */
    fun createSettingsIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.foodie.app.OPEN_SETTINGS"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Creates PendingIntent to retry meal analysis.
     *
     * Re-enqueues AnalyzeMealWorker with same input data (photo URI, timestamp).
     * Uses broadcast receiver to handle retry action outside MainActivity.
     *
     * @param workId Original work ID (for logging/tracking)
     * @param photoUri Photo URI to analyze
     * @param timestamp Meal timestamp in epoch seconds
     * @return PendingIntent for Retry action button
     */
    private fun createRetryIntent(workId: UUID, photoUri: String, timestamp: Long): PendingIntent {
        val intent = Intent(context, RetryAnalysisBroadcastReceiver::class.java).apply {
            action = "com.foodie.app.RETRY_ANALYSIS"
            putExtra("work_id", workId.toString())
            putExtra("photo_uri", photoUri)
            putExtra("timestamp", timestamp)
        }
        return PendingIntent.getBroadcast(
            context,
            workId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Creates PendingIntent to grant Health Connect permissions.
     *
     * Launches MainActivity which will trigger Health Connect permission flow.
     *
     * @return PendingIntent for Grant Access action button
     */
    private fun createPermissionsIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.foodie.app.GRANT_PERMISSIONS"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Creates PendingIntent to launch main activity (no specific action).
     *
     * Used as default content intent for notifications without action buttons.
     *
     * @return PendingIntent for main activity
     */
    private fun createMainActivityIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Gets notification title for error type.
     *
     * @param errorType The classified error
     * @return User-friendly title
     */
    private fun getErrorTitle(errorType: ErrorType): String {
        return when (errorType) {
            is ErrorType.NetworkError -> context.getString(R.string.notification_error_title_network)
            is ErrorType.ServerError -> context.getString(R.string.notification_error_title_server)
            is ErrorType.AuthError -> context.getString(R.string.notification_error_title_auth)
            is ErrorType.RateLimitError -> context.getString(R.string.notification_error_title_rate_limit)
            is ErrorType.ParseError -> context.getString(R.string.notification_error_title_parse)
            is ErrorType.PermissionDenied -> context.getString(R.string.notification_error_title_permission)
            is ErrorType.CameraPermissionDenied -> "Camera Permission Required"
            is ErrorType.ApiKeyMissing -> "Configuration Required"
            is ErrorType.StorageFull -> "Storage Full"
            is ErrorType.ValidationError -> context.getString(R.string.notification_error_title_validation)
            is ErrorType.HealthConnectUnavailable -> context.getString(R.string.notification_error_title_health_connect)
            is ErrorType.UnknownError -> context.getString(R.string.notification_error_title_unknown)
        }
    }
    
    /**
     * Gets notification message for error type.
     *
     * Delegates to ErrorHandler for single source of truth.
     *
     * @param errorType The classified error
     * @return User-friendly message from ErrorHandler
     */
    private fun getErrorMessage(errorType: ErrorType): String {
        return errorHandler.getUserMessage(errorType)
    }
}
