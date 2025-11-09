package com.foodie.app.ui.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Broadcast receiver for MealCaptureWidget lifecycle events.
 *
 * This receiver handles system broadcasts for widget lifecycle events such as:
 * - Widget enabled/disabled
 * - Widget added/removed from home/lock screen
 * - Widget update requests
 * - Device boot completed (for widget persistence)
 *
 * Architecture:
 * - Extends GlanceAppWidgetReceiver for Jetpack Glance integration
 * - Registered in AndroidManifest.xml with widget intent filters
 * - Returns MealCaptureWidget instance for Glance framework
 *
 * Widget Lifecycle:
 * - onEnabled: Called when first widget instance is created
 * - onUpdate: Called when widget needs to be updated (not used for static widget)
 * - onDeleted: Called when a widget instance is removed
 * - onDisabled: Called when last widget instance is removed
 *
 * Note: This is a stateless widget (no periodic updates), so onUpdate
 * is minimal and only renders the static content.
 *
 * @see MealCaptureWidget for widget content and behavior
 */
class MealCaptureWidgetReceiver : GlanceAppWidgetReceiver() {

    /**
     * Provides the GlanceAppWidget instance for this receiver.
     *
     * The Glance framework calls this to get the widget implementation
     * that should be used for rendering and update handling.
     *
     * @return MealCaptureWidget instance
     */
    override val glanceAppWidget: GlanceAppWidget = MealCaptureWidget
}
