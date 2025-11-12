package com.foodie.app.ui.screens.mealdetail

import androidx.lifecycle.SavedStateHandle
import com.foodie.app.domain.usecase.UpdateMealEntryUseCase
import com.foodie.app.util.Result
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MealDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var updateMealEntryUseCase: UpdateMealEntryUseCase
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: MealDetailViewModel

    private val testRecordId = "record-123"
    private val testCalories = "650"
    private val testDescription = "Grilled chicken salad"
    private val testTimestamp = Instant.parse("2025-11-12T14:30:00Z")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        updateMealEntryUseCase = mock()

        savedStateHandle = SavedStateHandle(
            mapOf(
                "recordId" to testRecordId,
                "calories" to testCalories,
                "description" to testDescription,
                "timestamp" to testTimestamp.toEpochMilli()
            )
        )

        viewModel = MealDetailViewModel(savedStateHandle, updateMealEntryUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should contain navigation args`() {
        // Then
        val state = viewModel.uiState.value
        assertThat(state.recordId).isEqualTo(testRecordId)
        assertThat(state.calories).isEqualTo(testCalories)
        assertThat(state.description).isEqualTo(testDescription)
        assertThat(state.timestamp).isEqualTo(testTimestamp)
        assertThat(state.isSaving).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `CaloriesChanged should update calories and validate`() {
        // When
        viewModel.onEvent(MealDetailEvent.CaloriesChanged("500"))

        // Then
        val state = viewModel.uiState.value
        assertThat(state.calories).isEqualTo("500")
        assertThat(state.caloriesError).isNull()
    }

    @Test
    fun `CaloriesChanged with value less than 1 should show error`() {
        // When
        viewModel.onEvent(MealDetailEvent.CaloriesChanged("0"))

        // Then
        val state = viewModel.uiState.value
        assertThat(state.calories).isEqualTo("0")
        assertThat(state.caloriesError).isEqualTo("Calories must be at least 1")
    }

    @Test
    fun `CaloriesChanged with value greater than 5000 should show error`() {
        // When
        viewModel.onEvent(MealDetailEvent.CaloriesChanged("5001"))

        // Then
        val state = viewModel.uiState.value
        assertThat(state.calories).isEqualTo("5001")
        assertThat(state.caloriesError).isEqualTo("Calories cannot exceed 5000")
    }

    @Test
    fun `CaloriesChanged with empty value should show error`() {
        // When
        viewModel.onEvent(MealDetailEvent.CaloriesChanged(""))

        // Then
        val state = viewModel.uiState.value
        assertThat(state.calories).isEmpty()
        assertThat(state.caloriesError).isEqualTo("Calories are required")
    }

    @Test
    fun `CaloriesChanged should filter non-digit characters`() {
        // When
        viewModel.onEvent(MealDetailEvent.CaloriesChanged("1a2b3"))

        // Then
        val state = viewModel.uiState.value
        assertThat(state.calories).isEqualTo("123")
    }

    @Test
    fun `CaloriesChanged at boundary 1 should pass validation`() {
        // When
        viewModel.onEvent(MealDetailEvent.CaloriesChanged("1"))

        // Then
        val state = viewModel.uiState.value
        assertThat(state.calories).isEqualTo("1")
        assertThat(state.caloriesError).isNull()
    }

    @Test
    fun `CaloriesChanged at boundary 5000 should pass validation`() {
        // When
        viewModel.onEvent(MealDetailEvent.CaloriesChanged("5000"))

        // Then
        val state = viewModel.uiState.value
        assertThat(state.calories).isEqualTo("5000")
        assertThat(state.caloriesError).isNull()
    }

    @Test
    fun `DescriptionChanged should update description and validate`() {
        // When
        viewModel.onEvent(MealDetailEvent.DescriptionChanged("New description"))

        // Then
        val state = viewModel.uiState.value
        assertThat(state.description).isEqualTo("New description")
        assertThat(state.descriptionError).isNull()
    }

    @Test
    fun `DescriptionChanged with blank value should show error`() {
        // When
        viewModel.onEvent(MealDetailEvent.DescriptionChanged("   "))

        // Then
        val state = viewModel.uiState.value
        assertThat(state.description).isEqualTo("   ")
        assertThat(state.descriptionError).isEqualTo("Description is required")
    }

    @Test
    fun `DescriptionChanged with empty value should show error`() {
        // When
        viewModel.onEvent(MealDetailEvent.DescriptionChanged(""))

        // Then
        val state = viewModel.uiState.value
        assertThat(state.description).isEmpty()
        assertThat(state.descriptionError).isEqualTo("Description is required")
    }

    @Test
    fun `DescriptionChanged with 201 characters should truncate to 200`() {
        // Given
        val longDescription = "a".repeat(201)

        // When
        viewModel.onEvent(MealDetailEvent.DescriptionChanged(longDescription))

        // Then
        val state = viewModel.uiState.value
        assertThat(state.description).hasLength(200)
        assertThat(state.descriptionError).isNull()
    }

    @Test
    fun `DescriptionChanged with exactly 200 characters should pass validation`() {
        // Given
        val maxDescription = "a".repeat(200)

        // When
        viewModel.onEvent(MealDetailEvent.DescriptionChanged(maxDescription))

        // Then
        val state = viewModel.uiState.value
        assertThat(state.description).hasLength(200)
        assertThat(state.descriptionError).isNull()
    }

    @Test
    fun `SaveClicked with valid data should call use case and navigate back`() = runTest {
        // Given
        whenever(updateMealEntryUseCase(any(), any(), any(), any()))
            .thenReturn(Result.Success(Unit))

        // When
        viewModel.onEvent(MealDetailEvent.SaveClicked)
        advanceUntilIdle()

        // Then
        verify(updateMealEntryUseCase).invoke(
            recordId = testRecordId,
            calories = testCalories.toInt(),
            description = testDescription,
            timestamp = testTimestamp
        )
        val state = viewModel.uiState.value
        assertThat(state.isSaving).isFalse()
        assertThat(state.shouldNavigateBack).isTrue()
        assertThat(state.error).isNull()
        assertThat(state.successMessage).isEqualTo("Entry updated")
    }

    @Test
    fun `SaveClicked with validation errors should not call use case`() = runTest {
        // Given - invalid calories
        viewModel.onEvent(MealDetailEvent.CaloriesChanged("0"))

        // When
        viewModel.onEvent(MealDetailEvent.SaveClicked)
        advanceUntilIdle()

        // Then
        verify(updateMealEntryUseCase, never()).invoke(any(), any(), any(), any())
        val state = viewModel.uiState.value
        assertThat(state.shouldNavigateBack).isFalse()
        assertThat(state.successMessage).isNull()
    }

    @Test
    fun `SaveClicked with valid data should complete successfully`() = runTest {
        // Given
        whenever(updateMealEntryUseCase(any(), any(), any(), any()))
            .thenReturn(Result.Success(Unit))

        // When
        viewModel.onEvent(MealDetailEvent.SaveClicked)
        advanceUntilIdle()

        // Then
        verify(updateMealEntryUseCase).invoke(
            recordId = testRecordId,
            calories = testCalories.toInt(),
            description = testDescription,
            timestamp = testTimestamp
        )
        val state = viewModel.uiState.value
        assertThat(state.isSaving).isFalse()
        assertThat(state.shouldNavigateBack).isTrue()
        assertThat(state.error).isNull()
        assertThat(state.successMessage).isEqualTo("Entry updated")
    }

    @Test
    fun `SaveClicked with use case error should show error and not navigate`() = runTest {
        // Given
        val errorMessage = "Health Connect unavailable"
        whenever(updateMealEntryUseCase(any(), any(), any(), any()))
            .thenReturn(Result.Error(RuntimeException(errorMessage)))

        // When
        viewModel.onEvent(MealDetailEvent.SaveClicked)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state.isSaving).isFalse()
        assertThat(state.shouldNavigateBack).isFalse()
        assertThat(state.error).isEqualTo(errorMessage)
        assertThat(state.successMessage).isNull()
    }

    @Test
    fun `SaveClicked with permission error should surface friendly message`() = runTest {
        // Given
        val friendlyMessage = "Permission denied. Please grant Health Connect access in settings."
        whenever(updateMealEntryUseCase(any(), any(), any(), any()))
            .thenReturn(Result.Error(SecurityException("Permissions not granted"), friendlyMessage))

        // When
        viewModel.onEvent(MealDetailEvent.SaveClicked)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state.isSaving).isFalse()
        assertThat(state.shouldNavigateBack).isFalse()
        assertThat(state.error).isEqualTo(friendlyMessage)
        assertThat(state.successMessage).isNull()
    }

    @Test
    fun `SaveClicked with Health Connect unavailable should show guidance`() = runTest {
        // Given
        val friendlyMessage = "Health Connect is not available. Please install it from the Play Store."
        whenever(updateMealEntryUseCase(any(), any(), any(), any()))
            .thenReturn(Result.Error(IllegalStateException("Health Connect not available"), friendlyMessage))

        // When
        viewModel.onEvent(MealDetailEvent.SaveClicked)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertThat(state.isSaving).isFalse()
        assertThat(state.shouldNavigateBack).isFalse()
        assertThat(state.error).isEqualTo(friendlyMessage)
        assertThat(state.successMessage).isNull()
    }

    @Test
    fun `CancelClicked should navigate back without saving`() = runTest {
        // When
        viewModel.onEvent(MealDetailEvent.CancelClicked)

        // Then
        verify(updateMealEntryUseCase, never()).invoke(any(), any(), any(), any())
        val state = viewModel.uiState.value
        assertThat(state.shouldNavigateBack).isTrue()
        assertThat(state.successMessage).isNull()
    }

    @Test
    fun `ErrorDismissed should clear error message`() {
        // Given - set an error first
        viewModel.onEvent(MealDetailEvent.CaloriesChanged(""))

        // When
        viewModel.onEvent(MealDetailEvent.ErrorDismissed)

        // Then
        val state = viewModel.uiState.value
        assertThat(state.error).isNull()
    }

    @Test
    fun `onNavigationHandled should reset navigation flag`() {
        // Given
        viewModel.onEvent(MealDetailEvent.CancelClicked)
        assertThat(viewModel.uiState.value.shouldNavigateBack).isTrue()

        // When
        viewModel.onNavigationHandled()

        // Then
        val state = viewModel.uiState.value
        assertThat(state.shouldNavigateBack).isFalse()
    }

    @Test
    fun `hasErrors should return true when validation errors exist`() {
        // Given
        viewModel.onEvent(MealDetailEvent.CaloriesChanged("0"))

        // Then
        val state = viewModel.uiState.value
        assertThat(state.hasErrors()).isTrue()
    }

    @Test
    fun `hasErrors should return false when no validation errors`() {
        // Then
        val state = viewModel.uiState.value
        assertThat(state.hasErrors()).isFalse()
    }

    @Test
    fun `canSave should return true when form is valid and not saving`() {
        // Then
        val state = viewModel.uiState.value
        assertThat(state.canSave()).isTrue()
    }

    @Test
    fun `canSave should return false when validation errors exist`() {
        // Given
        viewModel.onEvent(MealDetailEvent.CaloriesChanged("0"))

        // Then
        val state = viewModel.uiState.value
        assertThat(state.canSave()).isFalse()
    }
}
