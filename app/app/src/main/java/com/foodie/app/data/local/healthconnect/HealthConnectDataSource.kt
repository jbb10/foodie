package com.foodie.app.data.local.healthconnect

import androidx.health.connect.client.records.NutritionRecord
import java.time.Instant

/**
 * Data source interface for Health Connect operations.
 *
 * Abstracts Health Connect SDK interactions to allow for easier testing and
 * potential alternative implementations. This interface follows the repository
 * pattern at the data source level.
 *
 * All methods are suspend functions for async execution. Implementations should
 * handle Health Connect permissions and availability checks.
 */
interface HealthConnectDataSource {
    /**
     * Queries nutrition records from Health Connect within a time range.
     *
     * @param startTime Start of the query time range (inclusive)
     * @param endTime End of the query time range (inclusive)
     * @return List of NutritionRecord objects from Health Connect
     * @throws SecurityException if Health Connect permissions are not granted
     * @throws IllegalStateException if Health Connect is not available
     */
    suspend fun queryNutritionRecords(startTime: Instant, endTime: Instant): List<NutritionRecord>

    /**
     * Inserts a new nutrition record into Health Connect.
     *
     * @param calories Energy content in kilocalories (kcal)
     * @param description Meal description/name
     * @param timestamp When the meal was consumed
     * @return The unique record ID assigned by Health Connect
     * @throws SecurityException if Health Connect permissions are not granted
     * @throws IllegalStateException if Health Connect is not available
     */
    suspend fun insertNutritionRecord(calories: Int, description: String, timestamp: Instant): String

    /**
     * Deletes a nutrition record from Health Connect.
     *
     * @param recordId The unique ID of the record to delete
     * @throws SecurityException if Health Connect permissions are not granted
     * @throws IllegalStateException if Health Connect is not available
     */
    suspend fun deleteNutritionRecord(recordId: String)

    /**
     * Checks if Health Connect permissions are granted.
     *
     * @return true if all required permissions are granted, false otherwise
     */
    suspend fun checkPermissions(): Boolean
}
