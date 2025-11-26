package com.foodie.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for EnergyBalance domain model.
 *
 * Tests verify computed properties (isDeficit, formattedDeficitSurplus) and
 * TDEE formula correctness.
 */
class EnergyBalanceTest {

    @Test
    fun `isDeficit returns true when deficitSurplus is positive`() {
        // Given: Energy balance with deficit (burning more than consuming)
        val energyBalance = EnergyBalance(
            bmr = 1500.0,
            neat = 400.0,
            activeCalories = 300.0,
            tdee = 2200.0,
            caloriesIn = 1700.0,
            deficitSurplus = 500.0, // Deficit: 2200 - 1700 = 500
        )

        // When: Check if in deficit
        val isDeficit = energyBalance.isDeficit

        // Then: Should return true
        assertThat(isDeficit).isTrue()
    }

    @Test
    fun `isDeficit returns false when deficitSurplus is zero`() {
        // Given: Energy balance with perfect balance
        val energyBalance = EnergyBalance(
            bmr = 1500.0,
            neat = 400.0,
            activeCalories = 300.0,
            tdee = 2200.0,
            caloriesIn = 2200.0,
            deficitSurplus = 0.0, // Balanced
        )

        // When: Check if in deficit
        val isDeficit = energyBalance.isDeficit

        // Then: Should return false (not a deficit, perfectly balanced)
        assertThat(isDeficit).isFalse()
    }

    @Test
    fun `isDeficit returns false when deficitSurplus is negative`() {
        // Given: Energy balance with surplus (consuming more than burning)
        val energyBalance = EnergyBalance(
            bmr = 1500.0,
            neat = 400.0,
            activeCalories = 300.0,
            tdee = 2200.0,
            caloriesIn = 2400.0,
            deficitSurplus = -200.0, // Surplus: 2200 - 2400 = -200
        )

        // When: Check if in deficit
        val isDeficit = energyBalance.isDeficit

        // Then: Should return false (surplus, not deficit)
        assertThat(isDeficit).isFalse()
    }

    @Test
    fun `formattedDeficitSurplus shows deficit format when positive`() {
        // Given: Energy balance with 500 kcal deficit
        val energyBalance = EnergyBalance(
            bmr = 1500.0,
            neat = 400.0,
            activeCalories = 300.0,
            tdee = 2200.0,
            caloriesIn = 1700.0,
            deficitSurplus = 500.0,
        )

        // When: Get formatted deficit/surplus string
        val formatted = energyBalance.formattedDeficitSurplus

        // Then: Should show deficit format
        assertThat(formatted).isEqualTo("-500 kcal deficit")
    }

    @Test
    fun `formattedDeficitSurplus shows surplus format when negative`() {
        // Given: Energy balance with 200 kcal surplus
        val energyBalance = EnergyBalance(
            bmr = 1500.0,
            neat = 400.0,
            activeCalories = 300.0,
            tdee = 2200.0,
            caloriesIn = 2400.0,
            deficitSurplus = -200.0,
        )

        // When: Get formatted deficit/surplus string
        val formatted = energyBalance.formattedDeficitSurplus

        // Then: Should show surplus format
        assertThat(formatted).isEqualTo("+200 kcal surplus")
    }

    @Test
    fun `formattedDeficitSurplus shows balanced format when zero`() {
        // Given: Energy balance with perfect balance
        val energyBalance = EnergyBalance(
            bmr = 1500.0,
            neat = 400.0,
            activeCalories = 300.0,
            tdee = 2200.0,
            caloriesIn = 2200.0,
            deficitSurplus = 0.0,
        )

        // When: Get formatted deficit/surplus string
        val formatted = energyBalance.formattedDeficitSurplus

        // Then: Should show balanced format
        assertThat(formatted).isEqualTo("0 kcal balanced")
    }

    @Test
    fun `tdee field equals sum of bmr plus neat plus activeCalories`() {
        // Given: Energy balance with known component values
        val bmr = 1500.0
        val neat = 400.0
        val activeCalories = 300.0
        val expectedTDEE = bmr + neat + activeCalories // 2200.0

        val energyBalance = EnergyBalance(
            bmr = bmr,
            neat = neat,
            activeCalories = activeCalories,
            tdee = expectedTDEE,
            caloriesIn = 1800.0,
            deficitSurplus = 400.0,
        )

        // When: Check TDEE field value
        val actualTDEE = energyBalance.tdee

        // Then: TDEE should equal sum of components
        assertThat(actualTDEE).isEqualTo(expectedTDEE)
        assertThat(actualTDEE).isEqualTo(2200.0)
    }

    @Test
    fun `deficitSurplus field equals tdee minus caloriesIn`() {
        // Given: Energy balance with known TDEE and CaloriesIn
        val tdee = 2200.0
        val caloriesIn = 1700.0
        val expectedDeficit = tdee - caloriesIn // 500.0

        val energyBalance = EnergyBalance(
            bmr = 1500.0,
            neat = 400.0,
            activeCalories = 300.0,
            tdee = tdee,
            caloriesIn = caloriesIn,
            deficitSurplus = expectedDeficit,
        )

        // When: Check deficitSurplus field value
        val actualDeficit = energyBalance.deficitSurplus

        // Then: Deficit/surplus should equal TDEE - CaloriesIn
        assertThat(actualDeficit).isEqualTo(expectedDeficit)
        assertThat(actualDeficit).isEqualTo(500.0)
    }

    @Test
    fun `formattedDeficitSurplus handles large deficit correctly`() {
        // Given: Energy balance with large 1500 kcal deficit (aggressive cut)
        val energyBalance = EnergyBalance(
            bmr = 1500.0,
            neat = 400.0,
            activeCalories = 300.0,
            tdee = 2200.0,
            caloriesIn = 700.0, // Very low calorie diet
            deficitSurplus = 1500.0,
        )

        // When: Get formatted deficit/surplus string
        val formatted = energyBalance.formattedDeficitSurplus

        // Then: Should show large deficit format
        assertThat(formatted).isEqualTo("-1500 kcal deficit")
    }

    @Test
    fun `formattedDeficitSurplus handles large surplus correctly`() {
        // Given: Energy balance with large 1000 kcal surplus (bulking)
        val energyBalance = EnergyBalance(
            bmr = 1500.0,
            neat = 400.0,
            activeCalories = 300.0,
            tdee = 2200.0,
            caloriesIn = 3200.0, // High calorie diet
            deficitSurplus = -1000.0,
        )

        // When: Get formatted deficit/surplus string
        val formatted = energyBalance.formattedDeficitSurplus

        // Then: Should show large surplus format
        assertThat(formatted).isEqualTo("+1000 kcal surplus")
    }
}
