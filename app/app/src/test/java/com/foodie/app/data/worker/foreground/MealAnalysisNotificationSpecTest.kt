package com.foodie.app.data.worker.foreground

import android.app.NotificationManager
import org.junit.Assert.assertEquals
import org.junit.Test

class MealAnalysisNotificationSpecTest {

    @Test
    fun channelSpec_hasExpectedValues() {
        val spec = MealAnalysisNotificationSpec.channelSpec()

        assertEquals(MealAnalysisNotificationSpec.CHANNEL_ID, spec.id)
        assertEquals("Meal Analysis", spec.name)
        assertEquals("Silent notifications for background meal logging.", spec.description)
        assertEquals(NotificationManager.IMPORTANCE_LOW, spec.importance)
        assertEquals(false, spec.showBadge)
        assertEquals(false, spec.enableVibration)
        assertEquals(false, spec.enableSound)
    }
}
