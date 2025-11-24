package com.foodie.app.data.repository

import com.foodie.app.data.local.healthconnect.HealthConnectDataSource
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.domain.repository.MealRepository
import com.foodie.app.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MealRepository using Health Connect as the data source.
 *
 * This repository coordinates between the domain layer (business logic) and the
 * Health Connect data source (Android health data). It handles:
 * - Data transformation (Health Connect records → domain models)
 * - Error handling with Result wrapper
 * - Logging for debugging
 *
 * All operations use Health Connect as the single source of truth.
 */
@Singleton
class MealRepositoryImpl @Inject constructor(
    private val healthConnectDataSource: HealthConnectDataSource
) : MealRepository {

    companion object {
        private const val ERROR_PERMISSIONS_NOT_GRANTED = 
            "Health Connect permissions not granted. Please grant permissions in Settings."
        private const val ERROR_HEALTH_CONNECT_UNAVAILABLE = 
            "Health Connect is not available on this device."
    }

    override fun getMealHistory(): Flow<Result<List<MealEntry>>> = flow {
        emit(Result.Loading)
        
        try {
            // Query last 30 days of nutrition records
            val endTime = Instant.now()
            val startTime = endTime.minus(30, ChronoUnit.DAYS)
            
            Timber.d("Fetching meal history from $startTime to $endTime")
            
            val records = healthConnectDataSource.queryNutritionRecords(startTime, endTime)
            
            // Transform Health Connect records to domain models
            val mealEntries = records.mapNotNull { record ->
                try {
                    val calories = record.energy?.inKilocalories?.toInt() ?: 0
                    val description = record.name ?: "Unnamed meal"
                    
                    // Skip entries with invalid data
                    if (calories < 1 || calories > 5000) {
                        Timber.w("Skipping record with invalid calories: $calories")
                        return@mapNotNull null
                    }
                    
                    MealEntry(
                        id = record.metadata.id,
                        timestamp = record.startTime,
                        description = description,
                        calories = calories
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to transform nutrition record to MealEntry")
                    null
                }
            }.sortedByDescending { it.timestamp }
            
            Timber.d("Successfully fetched ${mealEntries.size} meal entries")
            emit(Result.Success(mealEntries))
            
        } catch (e: SecurityException) {
            Timber.e(e, ERROR_PERMISSIONS_NOT_GRANTED)
            emit(Result.Error(e, ERROR_PERMISSIONS_NOT_GRANTED))
        } catch (e: IllegalStateException) {
            Timber.e(e, ERROR_HEALTH_CONNECT_UNAVAILABLE)
            emit(Result.Error(e, ERROR_HEALTH_CONNECT_UNAVAILABLE))
        } catch (e: Exception) {
            val message = "Failed to fetch meal history. Please try again."
            Timber.e(e, message)
            emit(Result.Error(e, message))
        }
    }

    override suspend fun updateMeal(id: String, calories: Int, description: String): Result<Unit> {
        return try {
            Timber.d("Updating meal $id with $calories kcal, description: $description")
            
            // Validate input
            require(calories in 1..5000) { "Calories must be between 1 and 5000" }
            require(description.isNotBlank()) { "Description cannot be blank" }
            
            // Health Connect doesn't support updates, so we use delete + re-insert pattern
            // First, we need to query the original record to get its timestamp
            val endTime = Instant.now()
            val startTime = endTime.minus(30, ChronoUnit.DAYS)
            val records = healthConnectDataSource.queryNutritionRecords(startTime, endTime)
            val originalRecord = records.find { it.metadata.id == id }
                ?: return Result.Error(
                    IllegalArgumentException("Meal entry not found"),
                    "The meal you're trying to update was not found."
                )
            
            // Delete the old record
            healthConnectDataSource.deleteNutritionRecord(id)
            
            // Insert new record with updated values but original timestamp
            healthConnectDataSource.insertNutritionRecord(
                calories = calories,
                description = description,
                timestamp = originalRecord.startTime
            )
            
            Timber.d("Successfully updated meal $id")
            Result.Success(Unit)
            
        } catch (e: SecurityException) {
            Timber.e(e, ERROR_PERMISSIONS_NOT_GRANTED)
            Result.Error(e, ERROR_PERMISSIONS_NOT_GRANTED)
        } catch (e: IllegalStateException) {
            Timber.e(e, ERROR_HEALTH_CONNECT_UNAVAILABLE)
            Result.Error(e, ERROR_HEALTH_CONNECT_UNAVAILABLE)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, e.message)
            Result.Error(e, e.message ?: "Invalid input")
        } catch (e: Exception) {
            val message = "Failed to update meal. Please try again."
            Timber.e(e, message)
            Result.Error(e, message)
        }
    }

    override suspend fun deleteMeal(id: String): Result<Unit> {
        return try {
            Timber.d("Deleting meal $id")
            
            healthConnectDataSource.deleteNutritionRecord(id)
            
            Timber.d("Successfully deleted meal $id")
            Result.Success(Unit)
            
        } catch (e: SecurityException) {
            Timber.e(e, ERROR_PERMISSIONS_NOT_GRANTED)
            Result.Error(e, ERROR_PERMISSIONS_NOT_GRANTED)
        } catch (e: IllegalStateException) {
            Timber.e(e, ERROR_HEALTH_CONNECT_UNAVAILABLE)
            Result.Error(e, ERROR_HEALTH_CONNECT_UNAVAILABLE)
        } catch (e: Exception) {
            val message = "Failed to delete meal. Please try again."
            Timber.e(e, message)
            Result.Error(e, message)
        }
    }
}
