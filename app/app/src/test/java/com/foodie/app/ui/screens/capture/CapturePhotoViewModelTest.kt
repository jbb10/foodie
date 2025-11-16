package com.foodie.app.ui.screens.capture

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.WorkManager
import com.foodie.app.data.local.cache.PhotoManager
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * Unit tests for [CapturePhotoViewModel].
 *
 * Tests camera capture state management and coordination:
 * - Photo file preparation
 * - Capture success and failure handling
 * - Photo processing coordination
 * - Retake flow with cleanup
 *
 * Note: Permission checking tests require instrumentation tests due to
 * Android framework dependencies (ContextCompat.checkSelfPermission).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CapturePhotoViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockPhotoManager: PhotoManager
    
    @Mock
    private lateinit var mockWorkManager: WorkManager

    @Mock
    private lateinit var mockHealthConnectManager: HealthConnectManager

    private lateinit var viewModel: CapturePhotoViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = CapturePhotoViewModel(mockPhotoManager, mockWorkManager, mockHealthConnectManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isIdle() {
        // Then
        assertThat(viewModel.state.value).isInstanceOf(CaptureState.Idle::class.java)
    }

    @Test
    fun onPermissionGranted_preparesPhotoFile() = runTest {
        // Given
        val mockPhotoUri = mock<Uri>()
        whenever(mockPhotoManager.createPhotoFile()).thenReturn(mockPhotoUri)

        // When
        viewModel.onPermissionGranted()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(CaptureState.ReadyToCapture::class.java)
        verify(mockPhotoManager).createPhotoFile()
    }

    @Test
    fun onPermissionDenied_transitionsToPermissionDenied() = runTest {
        // When
        viewModel.onPermissionDenied()

        // Then
        assertThat(viewModel.state.value).isInstanceOf(CaptureState.PermissionDenied::class.java)
    }

    @Test
    fun onPhotoCaptured_processesPhoto() = runTest {
        // Given - Prepare capture first
        val mockOriginalUri = mock<Uri>()
        val mockProcessedUri = mock<Uri>()
        whenever(mockPhotoManager.createPhotoFile()).thenReturn(mockOriginalUri)
        whenever(mockPhotoManager.resizeAndCompress(mockOriginalUri)).thenReturn(mockProcessedUri)
        whenever(mockPhotoManager.deletePhoto(any())).thenReturn(true)

        viewModel.onPermissionGranted()
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Simulate successful capture
        viewModel.onPhotoCaptured()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should process photo and transition to ProcessingComplete
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(CaptureState.ProcessingComplete::class.java)
        assertThat((state as CaptureState.ProcessingComplete).processedPhotoUri)
            .isEqualTo(mockProcessedUri)

        // Should delete original unprocessed photo
        verify(mockPhotoManager).deletePhoto(mockOriginalUri)
    }

    @Test
    fun onPhotoCaptured_whenProcessingFails_transitionsToError() = runTest {
        // Given
        val mockOriginalUri = mock<Uri>()
        whenever(mockPhotoManager.createPhotoFile()).thenReturn(mockOriginalUri)
        whenever(mockPhotoManager.resizeAndCompress(mockOriginalUri)).thenReturn(null)
        whenever(mockPhotoManager.deletePhoto(any())).thenReturn(true)

        viewModel.onPermissionGranted()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onPhotoCaptured()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(CaptureState.Error::class.java)
        assertThat((state as CaptureState.Error).message).contains("process")
    }

    @Test
    fun onCaptureCancelled_cleansUpPhotoAndReturnsToIdle() = runTest {
        // Given
        val mockPhotoUri = mock<Uri>()
        whenever(mockPhotoManager.createPhotoFile()).thenReturn(mockPhotoUri)
        whenever(mockPhotoManager.deletePhoto(any())).thenReturn(true)

        viewModel.onPermissionGranted()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onCaptureCancelled()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertThat(viewModel.state.value).isInstanceOf(CaptureState.Idle::class.java)
        verify(mockPhotoManager).deletePhoto(mockPhotoUri)
    }

    @Test
    fun onRetake_deletesProcessedPhotoAndPreparesNewCapture() = runTest {
        // Given - Complete a capture first
        val mockOriginalUri = mock<Uri>()
        val mockProcessedUri = mock<Uri>()
        val mockNewPhotoUri = mock<Uri>()

        whenever(mockPhotoManager.createPhotoFile())
            .thenReturn(mockOriginalUri)
            .thenReturn(mockNewPhotoUri)
        whenever(mockPhotoManager.resizeAndCompress(mockOriginalUri)).thenReturn(mockProcessedUri)
        whenever(mockPhotoManager.deletePhoto(any())).thenReturn(true)

        viewModel.onPermissionGranted()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onPhotoCaptured()
        testDispatcher.scheduler.advanceUntilIdle()

        // When - User taps retake
        viewModel.onRetake()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(CaptureState.ReadyToCapture::class.java)
        assertThat((state as CaptureState.ReadyToCapture).photoUri).isEqualTo(mockNewPhotoUri)

        // Should delete processed photo
        verify(mockPhotoManager).deletePhoto(mockProcessedUri)
    }
}
