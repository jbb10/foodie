package com.foodie.app.domain.model

/**
 * Complete energy balance data for daily caloric tracking.
 *
 * Captures all components of the energy balance equation:
 * - **Calories Out (TDEE):** Total Daily Energy Expenditure = BMR + NEAT + Active
 * - **Calories In:** Total food/beverage calories consumed (from Health Connect nutrition records)
 * - **Deficit/Surplus:** Difference between Calories Out and Calories In
 * - **Macros:** Daily totals for protein, carbs, and fat (from nutrition records)
 *
 * **TDEE Components:**
 * - BMR (Basal Metabolic Rate): Energy burned at rest (Mifflin-St Jeor equation)
 * - NEAT (Non-Exercise Activity Thermogenesis): Energy from daily movement (steps × 0.04 kcal/step)
 * - Active: Energy from structured exercise (Health Connect ActiveCaloriesBurnedRecord)
 *
 * **Deficit/Surplus Calculation:**
 * - Deficit: deficitSurplus > 0 (burning more than consuming)
 * - Surplus: deficitSurplus < 0 (consuming more than burning)
 * - Formula: deficitSurplus = tdee - caloriesIn
 *
 * **Macros Tracking (Epic 7):**
 * - Protein: Total protein consumed in grams (0-500g typical range)
 * - Carbs: Total carbohydrates consumed in grams (0-1000g typical range)
 * - Fat: Total fat consumed in grams (0-500g typical range)
 * - Aggregated from Health Connect NutritionRecord.protein/totalCarbohydrate/totalFat fields
 *
 * **Reactive Updates:**
 * This model is emitted via Flow whenever ANY component changes:
 * - BMR: User profile updates (weight, age, height, sex)
 * - NEAT: Step count updates from Health Connect (polled every 5 minutes)
 * - Active: Workout calories sync from Health Connect (polled every 5 minutes)
 * - CaloriesIn/Macros: Meal entries added/edited/deleted in Health Connect
 *
 * @property bmr Basal Metabolic Rate in kcal/day (1200-2500 typical range)
 * @property neat Non-Exercise Activity Thermogenesis in kcal/day (0-800 typical range)
 * @property activeCalories Active exercise calories in kcal/day (0-3000 typical range)
 * @property tdee Total Daily Energy Expenditure in kcal/day (bmr + neat + activeCalories)
 * @property caloriesIn Total calories consumed today in kcal (from nutrition records)
 * @property deficitSurplus Caloric deficit (positive) or surplus (negative) in kcal (tdee - caloriesIn)
 * @property totalProtein Total protein consumed today in grams (Epic 7)
 * @property totalCarbs Total carbohydrates consumed today in grams (Epic 7)
 * @property totalFat Total fat consumed today in grams (Epic 7)
 */
data class EnergyBalance(
    val bmr: Double,
    val neat: Double,
    val activeCalories: Double,
    val tdee: Double,
    val caloriesIn: Double,
    val deficitSurplus: Double,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
) {
    /**
     * Whether the user is in a caloric deficit (burning more than consuming).
     *
     * @return true if deficitSurplus > 0 (deficit), false otherwise (surplus or balanced)
     */
    val isDeficit: Boolean
        get() = deficitSurplus > 0

    /**
     * Human-readable formatted deficit or surplus string.
     *
     * Examples:
     * - "-500 kcal deficit" (burning 500 more than consuming)
     * - "+200 kcal surplus" (consuming 200 more than burning)
     * - "0 kcal balanced" (perfectly balanced)
     *
     * @return Formatted string with sign, magnitude, and "kcal deficit/surplus/balanced"
     */
    val formattedDeficitSurplus: String
        get() = when {
            deficitSurplus > 0 -> "-${deficitSurplus.toInt()} kcal deficit"
            deficitSurplus < 0 -> "+${(-deficitSurplus).toInt()} kcal surplus"
            else -> "0 kcal balanced"
        }
}
