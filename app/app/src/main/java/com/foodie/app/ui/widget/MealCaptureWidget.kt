package com.foodie.app.ui.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import com.foodie.app.MainActivity
import com.foodie.app.R

/**
 * Home screen widget for quick meal capture.
 *
 * Provides a quick action button on the home screen that launches the camera
 * via deep linking after device unlock. This widget enables fast meal logging
 * with minimal friction, targeting sub-3-second launch time with biometric unlock.
 *
 * Architecture:
 * - Uses Jetpack Glance for modern Compose-like widget API
 * - Stateless widget (no periodic updates needed)
 * - Deep links to foodie://capture for camera navigation
 * PendingIntent configured for Android 12+ security compliance
 *
 * Widget Behaviour:
 * - Tap → Launches MainActivity with deep link intent
 * - MainActivity navigates to camera via NavGraph deep link handling
 * - Requires device unlock (home screen widget)
 * - Persists across device reboots
 *
 * Performance Target: < 3 seconds from device wake to camera ready (with biometric unlock)
 *
 * Platform Note: Android does not support third-party lock screen widgets on phones.
 * Lock screen shortcuts are limited to system apps only.
 *
 * @see MealCaptureWidgetReceiver for widget lifecycle handling
 * @see MainActivity for deep link navigation handling
 */
object MealCaptureWidget : GlanceAppWidget() {

    /**
     * Deep link URI for camera capture.
     * This URI is handled by NavGraph to navigate to the camera destination.
     */
    private const val DEEP_LINK_URI = "foodie://capture"

    /**
     * Provides the widget content using Glance Compose API.
     *
     * This method is called when the widget needs to be rendered or updated.
     * Creates a simple UI with app icon and "Log Meal" text that triggers
     * deep link navigation on tap.
     *
     * @param context Android context for creating PendingIntent
     * @param id Unique identifier for this widget instance
     */
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                widgetContent(context)
            }
        }
    }

    /**
     * Composable widget content with Material3 theming.
     *
     * Displays the app icon as a 1x1 clickable widget.
     * The entire widget is clickable and launches the deep link intent.
     *
     * @param context Android context for creating navigation intent
     */
    @Composable
    private fun widgetContent(context: Context) {
        // Simple 1x1 icon widget - uses dedicated widget icon
        Image(
            provider = ImageProvider(R.mipmap.ic_widget_foreground),
            contentDescription = "Log Meal",
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(onClick = actionStartActivity(createDeepLinkIntent(context))),
            contentScale = ContentScale.Fit,
        )
    }

    /**
     * Creates an Intent with deep link URI for camera navigation.
     *
     * This intent launches MainActivity with the foodie://capture deep link,
     * which is handled by NavGraph to navigate to the camera destination.
     *
     * Intent configuration:
     * - Action: VIEW (standard deep link action)
     * - Data: foodie://capture URI
     * - Flags: NEW_TASK | CLEAR_TASK (ensure clean navigation stack)
     *
     * The MainActivity will be launched with this intent, and its NavGraph
     * will handle the deep link routing to the camera screen.
     *
     * @param context Android context for creating intent
     * @return Intent configured for deep link navigation
     */
    private fun createDeepLinkIntent(context: Context): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(DEEP_LINK_URI)
            setClass(context, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }
}
