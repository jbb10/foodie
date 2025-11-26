package com.foodie.app.data.repository

import com.foodie.app.domain.model.MealEntry
import com.foodie.app.util.Result
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of HealthConnectRepository for testing.
 *
 * Provides in-memory storage of meal entries without requiring Health Connect SDK.
 * Used in instrumentation tests to provide predictable, controllable data.
 *
 * Pattern: Based on android/architecture-samples FakeTaskRepository
 *
 * Note: This is a separate implementation, not extending HealthConnectRepository,
 * because the production class is concrete with constructor dependencies.
 * Tests will pass this fake directly to ViewModels.
 */
class FakeHealthConnectRepository {

    private val meals = mutableMapOf<String, MealEntry>()
    private val _savedMeals = MutableStateFlow<Map<String, MealEntry>>(emptyMap())
    val savedMeals: StateFlow<Map<String, MealEntry>> = _savedMeals.asStateFlow()

    var shouldReturnError = false
    var errorMessage = "Test error"

    suspend fun insertNutritionRecord(
        calories: Int,
        description: String,
        timestamp: Instant,
    ): Result<String> {
        if (shouldReturnError) {
            return Result.Error(Exception(errorMessage), errorMessage)
        }

        val id = UUID.randomUUID().toString()
        val entry = MealEntry(
            id = id,
            description = description,
            calories = calories,
            timestamp = timestamp,
        )
        meals[id] = entry
        updateSavedMeals()
        return Result.Success(id)
    }

    suspend fun queryNutritionRecords(
        startTime: Instant,
        endTime: Instant,
    ): Result<List<MealEntry>> {
        if (shouldReturnError) {
            return Result.Error(Exception(errorMessage), errorMessage)
        }

        val filteredMeals = meals.values.filter { meal ->
            !meal.timestamp.isBefore(startTime) && !meal.timestamp.isAfter(endTime)
        }
        return Result.Success(filteredMeals)
    }

    suspend fun updateNutritionRecord(
        recordId: String,
        calories: Int,
        description: String,
        timestamp: Instant,
    ): Result<Unit> {
        if (shouldReturnError) {
            return Result.Error(Exception(errorMessage), errorMessage)
        }

        if (!meals.containsKey(recordId)) {
            return Result.Error(Exception("Record not found"), "Record not found")
        }

        val updated = MealEntry(
            id = recordId,
            description = description,
            calories = calories,
            timestamp = timestamp,
        )
        meals[recordId] = updated
        updateSavedMeals()
        return Result.Success(Unit)
    }

    suspend fun deleteNutritionRecord(recordId: String): Result<Unit> {
        if (shouldReturnError) {
            return Result.Error(Exception(errorMessage), errorMessage)
        }

        if (meals.remove(recordId) == null) {
            return Result.Error(Exception("Record not found"), "Record not found")
        }

        updateSavedMeals()
        return Result.Success(Unit)
    }

    // Test helpers

    fun addMeals(vararg entries: MealEntry) {
        entries.forEach { meals[it.id] = it }
        updateSavedMeals()
    }

    fun clearMeals() {
        meals.clear()
        updateSavedMeals()
    }

    private fun updateSavedMeals() {
        _savedMeals.value = meals.toMap()
    }
}
