package com.foodie.app.data.local.healthconnect

import androidx.health.connect.client.records.NutritionRecord
import com.foodie.app.domain.model.MealEntry

/**
 * Extension functions for Health Connect data model conversions.
 *
 * These functions convert between Health Connect SDK types (NutritionRecord)
 * and domain models (MealEntry), maintaining separation of concerns between
 * data and domain layers.
 */

/**
 * Converts a Health Connect NutritionRecord to domain MealEntry model.
 *
 * Mapping:
 * - NutritionRecord.metadata.id → MealEntry.id (or temporary ID if not yet saved)
 * - NutritionRecord.startTime → MealEntry.timestamp
 * - NutritionRecord.name → MealEntry.description
 * - NutritionRecord.energy (in kcal) → MealEntry.calories
 *
 * @return MealEntry domain model
 * @throws IllegalArgumentException if calories or description fail MealEntry validation
 */
fun NutritionRecord.toDomainModel(): MealEntry {
    return MealEntry(
        id = metadata.id.ifEmpty { "temp-${startTime.toEpochMilli()}" },
        timestamp = startTime,
        description = name ?: "",
        calories = energy?.inKilocalories?.toInt() ?: 0
    )
}
