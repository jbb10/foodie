package com.foodie.app.ui.screens.mealdetail

/**
 * User interaction events for the meal detail screen.
 *
 * Represents all possible actions a user can take on the edit form.
 */
sealed interface MealDetailEvent {
    /**
     * User modified the calories field.
     */
    data class CaloriesChanged(val value: String) : MealDetailEvent

    /**
     * User modified the description field.
     */
    data class DescriptionChanged(val value: String) : MealDetailEvent

    /**
     * User tapped the Save button.
     */
    data object SaveClicked : MealDetailEvent

    /**
     * User tapped the Cancel button.
     */
    data object CancelClicked : MealDetailEvent

    /**
     * User dismissed an error message.
     */
    data object ErrorDismissed : MealDetailEvent
}
