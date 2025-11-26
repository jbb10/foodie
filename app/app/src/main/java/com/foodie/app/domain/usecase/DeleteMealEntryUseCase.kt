package com.foodie.app.domain.usecase

import com.foodie.app.domain.repository.MealRepository
import com.foodie.app.util.Result
import javax.inject.Inject

/**
 * Use case for deleting a meal entry from Health Connect.
 *
 * This is a destructive operation that permanently removes the entry from Health Connect.
 * The deletion is atomic (single operation) and cannot be undone.
 *
 * @param mealRepository Repository providing access to meal data operations
 */
class DeleteMealEntryUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {
    /**
     * Deletes a meal entry by ID.
     *
     * @param mealId The unique identifier of the meal entry to delete
     * @return Result.Success(Unit) if deletion succeeds, Result.Error with user-friendly message if it fails
     *
     * Possible error scenarios:
     * - SecurityException: Health Connect permissions not granted
     * - IllegalStateException: Health Connect not available on device
     * - Exception: Network or system errors during deletion
     */
    suspend operator fun invoke(mealId: String): Result<Unit> {
        return mealRepository.deleteMeal(mealId)
    }
}
