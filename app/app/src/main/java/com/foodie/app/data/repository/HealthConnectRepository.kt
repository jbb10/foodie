package com.foodie.app.data.repository

import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.data.local.healthconnect.toDomainModel
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.util.Result
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

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
    private val healthConnectManager: HealthConnectManager
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
        timestamp: Instant
    ): Result<String> {
        return try {
            val recordId = healthConnectManager.insertNutritionRecord(calories, description, timestamp)
            Timber.tag(TAG).d("Insert successful: $recordId")
            Result.Success(recordId)
        } catch (e: SecurityException) {
            Timber.tag(TAG).e(e, "Insert failed: permissions not granted")
            Result.Error(e, "Health Connect permissions required. Please grant access in settings.")
        } catch (e: IllegalStateException) {
            Timber.tag(TAG).e(e, "Insert failed: Health Connect not available")
            Result.Error(e, "Health Connect is not available on this device.")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Insert failed: unexpected error")
            Result.Error(e, "Failed to save meal entry: ${e.localizedMessage}")
        }
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
        endTime: Instant
    ): Result<List<MealEntry>> {
        return try {
            val records = healthConnectManager.queryNutritionRecords(startTime, endTime)
            val mealEntries = records.map { it.toDomainModel() }
            Timber.tag(TAG).d("Query successful: ${mealEntries.size} entries")
            Result.Success(mealEntries)
        } catch (e: SecurityException) {
            Timber.tag(TAG).e(e, "Query failed: permissions not granted")
            Result.Error(e, "Health Connect permissions required. Please grant access in settings.")
        } catch (e: IllegalStateException) {
            Timber.tag(TAG).e(e, "Query failed: Health Connect not available")
            Result.Error(e, "Health Connect is not available on this device.")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Query failed: unexpected error")
            Result.Error(e, "Failed to load meal entries: ${e.localizedMessage}")
        }
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
        timestamp: Instant
    ): Result<Unit> {
        return try {
            healthConnectManager.updateNutritionRecord(recordId, calories, description, timestamp)
            Timber.tag(TAG).d("Update successful: $recordId")
            Result.Success(Unit)
        } catch (e: SecurityException) {
            Timber.tag(TAG).e(e, "Update failed: permissions not granted")
            Result.Error(e, "Health Connect permissions required. Please grant access in settings.")
        } catch (e: IllegalStateException) {
            Timber.tag(TAG).e(e, "Update failed: Health Connect not available")
            Result.Error(e, "Health Connect is not available on this device.")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Update failed: unexpected error")
            Result.Error(e, "Failed to update meal entry: ${e.localizedMessage}")
        }
    }

    /**
     * Deletes a meal entry from Health Connect.
     *
     * @param recordId The unique ID of the record to delete
     * @return Result.Success on completion, or Result.Error on failure
     */
    suspend fun deleteNutritionRecord(recordId: String): Result<Unit> {
        return try {
            healthConnectManager.deleteNutritionRecord(recordId)
            Timber.tag(TAG).d("Delete successful: $recordId")
            Result.Success(Unit)
        } catch (e: SecurityException) {
            Timber.tag(TAG).e(e, "Delete failed: permissions not granted")
            Result.Error(e, "Health Connect permissions required. Please grant access in settings.")
        } catch (e: IllegalStateException) {
            Timber.tag(TAG).e(e, "Delete failed: Health Connect not available")
            Result.Error(e, "Health Connect is not available on this device.")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Delete failed: unexpected error")
            Result.Error(e, "Failed to delete meal entry: ${e.localizedMessage}")
        }
    }
}
