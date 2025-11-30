package com.foodie.app.domain.usecase

import com.foodie.app.data.repository.HealthConnectRepository
import com.foodie.app.util.Result
import java.time.Instant
import javax.inject.Inject

/**
 * Use case to update an existing meal entry in Health Connect.
 *
 * This use case validates input parameters before delegating to the repository.
 * Ensures business rules are enforced at the domain layer before persisting changes.
 *
 * Validation Rules:
 * - Calories must be between 1 and 5000 (inclusive)
 * - Protein must be between 0 and 500g (inclusive)
 * - Carbs must be between 0 and 1000g (inclusive)
 * - Fat must be between 0 and 500g (inclusive)
 * - Description must not be blank and max 200 characters
 *
 * @param healthConnectRepository Repository for Health Connect operations
 */
class UpdateMealEntryUseCase @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository,
) {
    /**
     * Invokes the use case to update a meal entry.
     *
     * @param recordId The unique ID of the record to update
     * @param calories New energy content in kilocalories (1-5000)
     * @param protein New protein content in grams (0-500, supports up to 2 decimal places)
     * @param carbs New carbohydrate content in grams (0-1000, supports up to 2 decimal places)
     * @param fat New fat content in grams (0-500, supports up to 2 decimal places)
     * @param description New meal description (non-blank, max 200 chars)
     * @param timestamp Original timestamp (preserved in update)
     * @return Result.Success on completion, or Result.Error with validation/persistence failure
     */
    suspend operator fun invoke(
        recordId: String,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        description: String,
        timestamp: Instant,
    ): Result<Unit> {
        // Validate calories range
        if (calories !in 1..5000) {
            return Result.Error(
                IllegalArgumentException("Calories must be between 1 and 5000, got $calories"),
            )
        }

        // Validate protein range
        if (protein < 0.0 || protein > 500.0) {
            return Result.Error(
                IllegalArgumentException("Protein must be between 0 and 500g, got $protein"),
            )
        }

        // Validate carbs range
        if (carbs < 0.0 || carbs > 1000.0) {
            return Result.Error(
                IllegalArgumentException("Carbs must be between 0 and 1000g, got $carbs"),
            )
        }

        // Validate fat range
        if (fat < 0.0 || fat > 500.0) {
            return Result.Error(
                IllegalArgumentException("Fat must be between 0 and 500g, got $fat"),
            )
        }

        // Validate description is not blank
        if (description.isBlank()) {
            return Result.Error(
                IllegalArgumentException("Description cannot be blank"),
            )
        }

        // Validate description length
        if (description.length > 200) {
            return Result.Error(
                IllegalArgumentException("Description cannot exceed 200 characters, got ${description.length}"),
            )
        }

        // Delegate to repository for persistence
        return healthConnectRepository.updateNutritionRecord(
            recordId = recordId,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            description = description,
            timestamp = timestamp,
        )
    }
}
