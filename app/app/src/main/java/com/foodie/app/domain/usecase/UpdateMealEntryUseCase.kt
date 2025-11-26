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
     * @param description New meal description (non-blank, max 200 chars)
     * @param timestamp Original timestamp (preserved in update)
     * @return Result.Success on completion, or Result.Error with validation/persistence failure
     */
    suspend operator fun invoke(
        recordId: String,
        calories: Int,
        description: String,
        timestamp: Instant,
    ): Result<Unit> {
        // Validate calories range
        if (calories !in 1..5000) {
            return Result.Error(
                IllegalArgumentException("Calories must be between 1 and 5000, got $calories"),
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
            description = description,
            timestamp = timestamp,
        )
    }
}
