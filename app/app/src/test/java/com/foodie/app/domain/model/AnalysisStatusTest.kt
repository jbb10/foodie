package com.foodie.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for AnalysisStatus sealed class.
 */
class AnalysisStatusTest {

    @Test
    fun `Idle state should be a singleton object`() {
        // When
        val status1 = AnalysisStatus.Idle
        val status2 = AnalysisStatus.Idle

        // Then
        assertThat(status1).isSameInstanceAs(status2)
    }

    @Test
    fun `Analyzing state should hold progress value`() {
        // Given
        val progress = 0.5f

        // When
        val status = AnalysisStatus.Analyzing(progress)

        // Then
        assertThat(status.progress).isEqualTo(progress)
    }

    @Test
    fun `Analyzing state should allow null progress`() {
        // When
        val status = AnalysisStatus.Analyzing()

        // Then
        assertThat(status.progress).isNull()
    }

    @Test
    fun `Success state should hold NutritionData`() {
        // Given
        val nutritionData = NutritionData(calories = 450, description = "Chicken salad")

        // When
        val status = AnalysisStatus.Success(nutritionData)

        // Then
        assertThat(status.data).isEqualTo(nutritionData)
        assertThat(status.data.calories).isEqualTo(450)
        assertThat(status.data.description).isEqualTo("Chicken salad")
    }

    @Test
    fun `Error state should hold message and optional exception`() {
        // Given
        val message = "Analysis failed"
        val exception = RuntimeException("API error")

        // When
        val status = AnalysisStatus.Error(message, exception)

        // Then
        assertThat(status.message).isEqualTo(message)
        assertThat(status.exception).isEqualTo(exception)
    }

    @Test
    fun `Error state should allow null exception`() {
        // Given
        val message = "Analysis failed"

        // When
        val status = AnalysisStatus.Error(message)

        // Then
        assertThat(status.message).isEqualTo(message)
        assertThat(status.exception).isNull()
    }

    @Test
    fun `AnalysisStatus subclasses should be type-safe`() {
        // Given
        val statuses: List<AnalysisStatus> = listOf(
            AnalysisStatus.Idle,
            AnalysisStatus.Analyzing(0.5f),
            AnalysisStatus.Success(NutritionData(450, "Chicken salad")),
            AnalysisStatus.Error("Error message")
        )

        // When/Then
        statuses.forEach { status ->
            when (status) {
                is AnalysisStatus.Idle -> assertThat(status).isInstanceOf(AnalysisStatus.Idle::class.java)
                is AnalysisStatus.Analyzing -> assertThat(status.progress).isNotNull()
                is AnalysisStatus.Success -> assertThat(status.data).isNotNull()
                is AnalysisStatus.Error -> assertThat(status.message).isNotEmpty()
            }
        }
    }
}
