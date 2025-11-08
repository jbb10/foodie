package com.foodie.app.domain.repository

import com.foodie.app.domain.model.MealEntry
import com.foodie.app.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for meal entry data access.
 *
 * This interface defines the contract for accessing and managing meal entries,
 * abstracting away the underlying data source (Health Connect). All implementations
 * must handle errors gracefully and return Result wrappers.
 *
 * Implementation notes:
 * - All operations use Health Connect as the single source of truth
 * - Errors should be mapped to user-friendly messages
 * - Use Flow for continuous data streams that update reactively
 * - Update operations use delete + re-insert pattern (Health Connect limitation)
 */
interface MealRepository {
    /**
     * Retrieves the meal history as a reactive stream.
     *
     * Emits new values whenever the underlying Health Connect data changes.
     * Typically queries the last 30 days of meal entries.
     *
     * @return Flow emitting Result with list of meal entries, or Error on failure
     */
    fun getMealHistory(): Flow<Result<List<MealEntry>>>

    /**
     * Updates an existing meal entry with new calorie and description data.
     *
     * Implementation uses Health Connect's delete + re-insert pattern since
     * Health Connect doesn't support direct updates.
     *
     * @param id The unique identifier of the meal to update
     * @param calories The new calorie value (must be 1-5000)
     * @param description The new description (must be non-blank)
     * @return Result.Success on successful update, Result.Error on failure
     *
     * Possible errors:
     * - Meal entry not found
     * - Health Connect permission denied
     * - Network/system errors
     */
    suspend fun updateMeal(id: String, calories: Int, description: String): Result<Unit>

    /**
     * Deletes a meal entry from Health Connect.
     *
     * @param id The unique identifier of the meal to delete
     * @return Result.Success on successful deletion, Result.Error on failure
     *
     * Possible errors:
     * - Meal entry not found
     * - Health Connect permission denied
     * - Network/system errors
     */
    suspend fun deleteMeal(id: String): Result<Unit>
}
