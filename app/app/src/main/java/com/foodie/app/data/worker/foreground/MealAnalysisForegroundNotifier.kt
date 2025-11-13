package com.foodie.app.data.worker.foreground

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import com.foodie.app.MainActivity
import com.foodie.app.R
import com.foodie.app.domain.model.NutritionData
import com.foodie.app.ui.navigation.Screen
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Builds foreground and follow-up notifications for meal analysis runs.
 */
class MealAnalysisForegroundNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun createForegroundInfo(
        workId: UUID,
        statusText: String,
        progressPercent: Int? = null
    ): ForegroundInfo {
        val notification = baseBuilder()
            .setContentTitle(context.getString(R.string.notification_meal_analysis_title))
            .setContentText(statusText)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent())
            .apply {
                if (progressPercent != null) {
                    val bounded = progressPercent.coerceIn(0, 100)
                    setProgress(100, bounded, false)
                } else {
                    setProgress(0, 0, true)
                }
            }
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                MealAnalysisNotificationSpec.ONGOING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(MealAnalysisNotificationSpec.ONGOING_NOTIFICATION_ID, notification)
        }
    }

    fun createCompletionNotification(
        data: NutritionData,
        recordId: String,
        timestamp: Instant
    ): Notification {
        val body = context.getString(
            R.string.notification_meal_analysis_success_body,
            data.calories,
            data.description
        )

        // Create deep link to meal detail screen
        val detailPendingIntent = createMealDetailPendingIntent(recordId, data, timestamp)

        return baseBuilder()
            .setContentTitle(context.getString(R.string.notification_meal_analysis_success_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(detailPendingIntent)
            .build()
    }

    fun createFailureNotification(workId: UUID, errorReason: String): Notification {
        val body = context.getString(
            R.string.notification_meal_analysis_failure_body,
            errorReason
        )

        return baseBuilder()
            .setContentTitle(context.getString(R.string.notification_meal_analysis_failure_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent())
            .build()
    }

    private fun baseBuilder(): NotificationCompat.Builder {
        val accentColor = ContextCompat.getColor(context, R.color.purple_500)
        return NotificationCompat.Builder(context, MealAnalysisNotificationSpec.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(accentColor)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
    }

    private fun mainPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    private fun createMealDetailPendingIntent(
        recordId: String,
        data: NutritionData,
        timestamp: Instant
    ): PendingIntent {
        val route = Screen.MealDetail.createRoute(
            recordId = recordId,
            calories = data.calories,
            description = data.description,
            timestamp = timestamp.toEpochMilli()
        )
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Pass the route as an extra so MainActivity can navigate to it
            putExtra("navigate_to", route)
        }
        
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        // Use recordId hashCode as request code to ensure unique PendingIntents for different meals
        return PendingIntent.getActivity(context, recordId.hashCode(), intent, flags)
    }
}
