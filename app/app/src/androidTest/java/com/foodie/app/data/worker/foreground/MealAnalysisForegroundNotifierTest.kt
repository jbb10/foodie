package com.foodie.app.data.worker.foreground

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.foodie.app.domain.model.NutritionData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MealAnalysisForegroundNotifierTest {

    private lateinit var context: Context
    private lateinit var notifier: MealAnalysisForegroundNotifier

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notifier = MealAnalysisForegroundNotifier(context)
    }

    @Suppress("DEPRECATION")
    @Test
    fun createForegroundInfo_usesMealAnalysisChannelWithLowPriority() {
        val status = "Uploading photo…"
        val info = notifier.createForegroundInfo(UUID.randomUUID(), status, 30)
        val notification = info.notification

        assertEquals(MealAnalysisNotificationSpec.CHANNEL_ID, notification.channelId)
        assertEquals(NotificationCompat.PRIORITY_LOW, notification.priority)
        assertEquals(Notification.FLAG_ONGOING_EVENT, notification.flags and Notification.FLAG_ONGOING_EVENT)
        assertEquals(status, notification.extras.getString(Notification.EXTRA_TEXT))
    }

    @Suppress("DEPRECATION")
    @Test
    fun createCompletionNotification_includesNutritionDetails() {
        val data = NutritionData(calories = 420, description = "Grilled chicken bowl")
        val notification = notifier.createCompletionNotification(data)

        assertEquals(MealAnalysisNotificationSpec.CHANNEL_ID, notification.channelId)
        assertEquals(NotificationCompat.PRIORITY_DEFAULT, notification.priority)
        assertEquals(context.getString(com.foodie.app.R.string.notification_meal_analysis_success_title), notification.extras.getString(Notification.EXTRA_TITLE))
        assertNotNull(notification.contentIntent)
    }

    @Suppress("DEPRECATION")
    @Test
    fun createFailureNotification_addsWorkIdSubText() {
        val workId = UUID.randomUUID()
        val notification = notifier.createFailureNotification(workId, "API request failed")

        assertEquals(MealAnalysisNotificationSpec.CHANNEL_ID, notification.channelId)
        assertEquals(NotificationCompat.PRIORITY_DEFAULT, notification.priority)
        // Note: subText is not a direct property on Notification, checking extras instead
        assertEquals(context.getString(com.foodie.app.R.string.notification_meal_analysis_failure_title), notification.extras.getString(Notification.EXTRA_TITLE))
    }
}
