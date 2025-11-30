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
 * **Epic 7 Extension:** Extracts macros fields (protein, carbs, fat) from Health Connect,
 * defaulting to 0 for legacy records that don't have macros data.
 *
 * Mapping:
 * - NutritionRecord.metadata.id → MealEntry.id (or temporary ID if not yet saved)
 * - NutritionRecord.startTime → MealEntry.timestamp
 * - NutritionRecord.name → MealEntry.description (fallback: "Unknown meal" if blank)
 * - NutritionRecord.energy (in kcal) → MealEntry.calories
 * - NutritionRecord.protein (in grams) → MealEntry.protein (default: 0 for legacy records)
 * - NutritionRecord.totalCarbohydrate (in grams) → MealEntry.carbs (default: 0 for legacy records)
 * - NutritionRecord.totalFat (in grams) → MealEntry.fat (default: 0 for legacy records)
 *
 * @return MealEntry domain model
 * @throws IllegalArgumentException if calories, macros, or description fail MealEntry validation
 */
fun NutritionRecord.toDomainModel(): MealEntry {
    val description = name?.takeIf { it.isNotBlank() } ?: "Unknown meal"
    return MealEntry(
        id = metadata.id.ifEmpty { "temp-${startTime.toEpochMilli()}" },
        timestamp = startTime,
        description = description,
        calories = energy?.inKilocalories?.toInt() ?: 0,
        protein = protein?.inGrams ?: 0.0,
        carbs = totalCarbohydrate?.inGrams ?: 0.0,
        fat = totalFat?.inGrams ?: 0.0,
        zoneOffset = startZoneOffset,
    )
}
