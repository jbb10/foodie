package com.foodie.app.data.repository

import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.data.local.healthconnect.toDomainModel
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.util.Result
import com.foodie.app.util.logDebug
import com.foodie.app.util.runCatchingResult
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Repository for Health Connect nutrition data operations.
 *
 * Provides domain-level CRUD operations for meal entries, abstracting Health Connect
 * implementation details from ViewModels. All operations return Result<T> for
 * consistent error handling.
 *
 * Architecture: ViewModel → HealthConnectRepository → HealthConnectManager → HealthConnectClient (SDK)
 */
@Singleton
class HealthConnectRepository @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
) {
    companion object {
        private const val TAG = "HealthConnectRepository"
    }

    /**
     * Inserts a new meal entry into Health Connect.
     *
     * @param calories Energy content in kilocalories (kcal), must be 1-5000
     * @param description Meal description/name, must not be blank
     * @param timestamp When the meal was consumed
     * @return Result.Success with record ID, or Result.Error on failure
     */
    suspend fun insertNutritionRecord(
        calories: Int,
        description: String,
        timestamp: Instant,
    ): Result<String> = runCatchingResult {
        logDebug("Inserting nutrition record: $calories cal, $description")
        val recordId = healthConnectManager.insertNutritionRecord(calories, description, timestamp)
        Timber.tag(TAG).i("Insert successful: $recordId")
        recordId
    }

    /**
     * Queries meal entries from Health Connect within a time range.
     *
     * @param startTime Start of the query time range (inclusive)
     * @param endTime End of the query time range (inclusive)
     * @return Result.Success with list of MealEntry, or Result.Error on failure
     */
    suspend fun queryNutritionRecords(
        startTime: Instant,
        endTime: Instant,
    ): Result<List<MealEntry>> = runCatchingResult {
        logDebug("Querying nutrition records: $startTime to $endTime")
        val records = healthConnectManager.queryNutritionRecords(startTime, endTime)
        val mealEntries = records.map { it.toDomainModel() }
        Timber.tag(TAG).i("Query successful: ${mealEntries.size} entries")
        mealEntries
    }

    /**
     * Updates an existing meal entry in Health Connect.
     *
     * Note: Health Connect doesn't support direct updates. This uses delete + re-insert
     * with preserved timestamp.
     *
     * @param recordId The unique ID of the record to update
     * @param calories New energy content in kilocalories
     * @param description New meal description
     * @param timestamp Original timestamp (preserved)
     * @return Result.Success on completion, or Result.Error on failure
     */
    suspend fun updateNutritionRecord(
        recordId: String,
        calories: Int,
        description: String,
        timestamp: Instant,
    ): Result<Unit> = runCatchingResult {
        logDebug("Updating nutrition record: $recordId")
        healthConnectManager.updateNutritionRecord(recordId, calories, description, timestamp)
        Timber.tag(TAG).i("Update successful: $recordId")
    }

    /**
     * Deletes a meal entry from Health Connect.
     *
     * @param recordId The unique ID of the record to delete
     * @return Result.Success on completion, or Result.Error on failure
     */
    suspend fun deleteNutritionRecord(recordId: String): Result<Unit> = runCatchingResult {
        logDebug("Deleting nutrition record: $recordId")
        healthConnectManager.deleteNutritionRecord(recordId)
        Timber.tag(TAG).i("Delete successful: $recordId")
    }

    /**
     * Retrieves a flow of meal history from the last 7 days.
     *
     * @return A Flow that emits a Result containing a list of MealEntry objects.
     */
    fun getMealHistory(): Flow<Result<List<MealEntry>>> = flow {
        emit(Result.Loading)
        val result = runCatchingResult {
            logDebug("Fetching meal history for the last 7 days")
            val endTime = Instant.now()
            val startTime = endTime.minus(7, ChronoUnit.DAYS)
            val records = healthConnectManager.queryNutritionRecords(startTime, endTime)
            val mealEntries = records.map { it.toDomainModel() }
                .sortedByDescending { it.timestamp }
            Timber.tag(TAG).i("Meal history query successful: ${mealEntries.size} entries")
            mealEntries
        }
        emit(result)
    }
}
