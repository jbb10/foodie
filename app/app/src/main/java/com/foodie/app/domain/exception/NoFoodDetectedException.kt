package com.foodie.app.domain.exception

/**
 * Exception thrown when the AI detects no food in the analyzed image.
 *
 * This exception indicates that the user captured a photo that doesn't contain
 * recognizable food items. Common scenarios:
 * - Empty plate or table
 * - Non-food objects (documents, scenery, people, etc.)
 * - Low-quality or blurry images where food cannot be identified
 * - Accidentally triggered widget
 *
 * **Handling Strategy:**
 * - This is a non-retryable error (user action required)
 * - Display friendly message asking user to retake photo
 * - Delete the photo from temporary storage
 * - Don't save to Health Connect
 * - Don't retry with WorkManager (return Result.failure)
 *
 * **User Experience:**
 * - Show notification: "No food detected. Please take a photo of your meal."
 * - Optionally: Provide tips for better photos (good lighting, clear view of food)
 *
 * Example usage:
 * ```
 * when (result) {
 *     is Result.Success -> saveToHealthConnect(result.data)
 *     is Result.Error -> {
 *         if (result.exception is NoFoodDetectedException) {
 *             showNoFoodDetectedMessage()
 *         } else {
 *             handleGenericError(result)
 *         }
 *     }
 * }
 * ```
 *
 * @param message Optional custom message from the AI explaining what was detected instead
 */
class NoFoodDetectedException(
    message: String = "No food detected in the image",
) : Exception(message)
