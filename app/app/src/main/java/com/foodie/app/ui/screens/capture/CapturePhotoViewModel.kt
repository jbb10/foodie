package com.foodie.app.ui.screens.capture

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.foodie.app.R
import com.foodie.app.data.local.cache.PhotoManager
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.data.worker.AnalyzeMealWorker
import com.foodie.app.notifications.NotificationPermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * State for camera capture flow.
 *
 * Represents the different stages of photo capture from permission request
 * through processing to final confirmation.
 */
sealed class CaptureState {
    /** Initial state before permission check */
    data object Idle : CaptureState()

    /** Requesting Health Connect permissions (must happen before camera) */
    data object RequestingHealthConnectPermission : CaptureState()

    /** Health Connect permission denied by user */
    data object HealthConnectPermissionDenied : CaptureState()

    /** Requesting camera permission from user */
    data object RequestingPermission : CaptureState()

    /** Permission denied by user */
    data object PermissionDenied : CaptureState()

    /** Ready to launch camera with temporary file prepared */
    data class ReadyToCapture(val photoUri: Uri) : CaptureState()

    /** Processing captured photo (resize + compress) */
    data object Processing : CaptureState()

    /** Processing complete, showing preview */
    data class ProcessingComplete(val processedPhotoUri: Uri) : CaptureState()

    /** Background processing enqueued, user can return to previous activity */
    data object BackgroundProcessingStarted : CaptureState()

    /** Notification permission required before scheduling foreground work */
    data object NotificationPermissionRequired : CaptureState()

    /** Notification permission denied by user */
    data object NotificationPermissionDenied : CaptureState()

    /** Error occurred during capture or processing */
    data class Error(val message: String) : CaptureState()
}

/**
 * ViewModel managing camera capture state and coordination.
 *
 * Responsibilities:
 * - Check and request camera permission
 * - Create temporary photo file for camera intent
 * - Process captured photo (resize + compress via PhotoManager)
 * - Manage capture state transitions
 * - Handle retake flow (cleanup + reset)
 *
 * Architecture:
 * - Injected via Hilt with PhotoManager dependency
 * - Exposes StateFlow for reactive UI updates
 * - All file operations delegated to PhotoManager
 * - Suspend functions executed in viewModelScope
 *
 * State flow:
 * ```
 * Idle → RequestingPermission → ReadyToCapture → Processing → ProcessingComplete
 *   ↓                 ↓                               ↓
 * Error        PermissionDenied                    Error
 * ```
 *
 * @param photoManager PhotoManager for file operations
 * @param workManager WorkManager for background processing jobs
 */
@HiltViewModel
class CapturePhotoViewModel @Inject constructor(
    private val photoManager: PhotoManager,
    private val workManager: WorkManager,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    companion object {
        private const val TAG = "CapturePhotoViewModel"
    }

    private val _state = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val state: StateFlow<CaptureState> = _state.asStateFlow()

    private var currentPhotoUri: Uri? = null
    private var processedPhotoUri: Uri? = null
    
    // Performance tracking (Story 2-7, AC#1)
    private var screenLaunchTime: Long = 0L

    init {
        // Track screen launch time for widget → camera launch performance measurement
        screenLaunchTime = System.currentTimeMillis()
        Timber.d("CapturePhotoScreen launched at $screenLaunchTime")
    }

    /**
     * Checks Health Connect and camera permissions, then prepares for capture.
     *
     * Flow:
     * 1. Check Health Connect permissions first (required for saving meal data)
     * 2. If HC denied: Request HC permissions
     * 3. If HC granted: Check camera permission
     * 4. If camera denied: Request camera permission
     * 5. If all granted: Prepare for capture
     *
     * @param context Android context for permission checks
     */
    fun checkPermissionAndPrepare(context: Context) {
        viewModelScope.launch {
            // First, check Health Connect permissions
            val hasHealthConnectPermissions = healthConnectManager.checkPermissions()
            
            if (!hasHealthConnectPermissions) {
                Timber.i("Health Connect permissions missing - requesting before camera")
                _state.value = CaptureState.RequestingHealthConnectPermission
                return@launch
            }
            
            // HC permissions granted, now check camera permission
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCameraPermission) {
                prepareForCapture()
            } else {
                _state.value = CaptureState.RequestingPermission
            }
        }
    }

    /**
     * Called when Health Connect permissions are granted.
     * Proceeds to check camera permission.
     */
    fun onHealthConnectPermissionGranted(context: Context) {
        Timber.i("Health Connect permissions granted, checking camera permission")
        
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            prepareForCapture()
        } else {
            _state.value = CaptureState.RequestingPermission
        }
    }

    /**
     * Called when Health Connect permissions are denied.
     * Cannot proceed with capture flow.
     */
    fun onHealthConnectPermissionDenied() {
        Timber.w("Health Connect permissions denied - cannot save meals without them")
        _state.value = CaptureState.HealthConnectPermissionDenied
    }

    /**
     * Called when camera permission is granted.
     *
     * Prepares temporary file and transitions to ReadyToCapture.
     */
    fun onPermissionGranted() {
        Timber.d("Camera permission granted")
        prepareForCapture()
    }

    /**
     * Called when camera permission is denied.
     *
     * Transitions to PermissionDenied state for user guidance.
     */
    fun onPermissionDenied() {
        Timber.w("Camera permission denied")
        _state.value = CaptureState.PermissionDenied
    }

    /**
     * Called when photo is successfully captured by camera.
     *
     * Initiates photo processing (resize + compress).
     */
    fun onPhotoCaptured() {
        Timber.d("Photo captured, starting processing")
        _state.value = CaptureState.Processing
        processPhoto()
    }

    /**
     * Called when camera capture is cancelled by user.
     *
     * Cleans up temporary file and returns to idle.
     */
    fun onCaptureCancelled() {
        Timber.d("Capture cancelled")
        cleanupCurrentPhoto()
        _state.value = CaptureState.Idle
    }

    /**
     * Called when user taps Retake button.
     *
     * Deletes processed photo and prepares for new capture.
     */
    fun onRetake() {
        Timber.d("Retake requested")
        cleanupProcessedPhoto()
        prepareForCapture()
    }

    /**
     * Called when user taps Use Photo button.
     *
     * Enqueues background processing job with WorkManager. If notification
     * permission is missing (shouldn't happen if first-launch flow completed),
     * prompts user to grant it.
     *
     * WorkManager job will:
     * 1. Call Azure OpenAI API for nutrition analysis
     * 2. Save results to Health Connect
     * 3. Delete temporary photo file
     * 4. Retry on network failures with exponential backoff (1s, 2s, 4s delays)
     */
    fun onUsePhoto(context: Context, skipPermissionCheck: Boolean = false) {
        Timber.d("Use photo requested (skipPermissionCheck=%s)", skipPermissionCheck)

        viewModelScope.launch {
            // Only check notification permission if user skipped first-launch flow
            // (e.g., went straight to widget without opening app)
            if (!skipPermissionCheck &&
                NotificationPermissionManager.isPermissionRequired() &&
                !hasNotificationPermission(context)
            ) {
                Timber.w("Notification permission missing - prompting user (first launch may have been skipped)")
                _state.value = CaptureState.NotificationPermissionRequired
                return@launch
            }

            enqueueAnalysisWork()
        }
    }

    fun onNotificationPermissionResult(granted: Boolean, context: Context) {
        if (granted) {
            Timber.d("Notification permission granted for foreground work")
            // Proceed with analysis now that permission is granted
            onUsePhoto(context, skipPermissionCheck = true)
        } else {
            Timber.w("Notification permission denied; user must enable before proceeding")
            _state.value = CaptureState.NotificationPermissionDenied
        }
    }

    fun isNotificationPermissionRequired(): Boolean = NotificationPermissionManager.isPermissionRequired()

    fun getProcessedPhotoUri(): Uri? = processedPhotoUri

    fun retryNotificationPermission() {
        _state.value = CaptureState.NotificationPermissionRequired
    }

    /**
     * Creates permission request contract for Health Connect.
     * Exposes the contract from HealthConnectManager for use in Composable.
     */
    fun createHealthConnectPermissionContract() = 
        healthConnectManager.createPermissionRequestContract()

    private fun hasNotificationPermission(context: Context): Boolean {
        val granted = NotificationPermissionManager.hasPermission(context)
        if (!granted) {
            Timber.w("Notification permission missing for foreground WorkManager execution")
        }
        return granted
    }

    private suspend fun enqueueAnalysisWork() {
        try {
            val photoUri = processedPhotoUri
                ?: throw IllegalStateException("No processed photo URI available")

            val timestamp = Instant.now()

            val workRequest = OneTimeWorkRequestBuilder<AnalyzeMealWorker>()
                .setInputData(
                    workDataOf(
                        AnalyzeMealWorker.KEY_PHOTO_URI to photoUri.toString(),
                        AnalyzeMealWorker.KEY_TIMESTAMP to timestamp.epochSecond
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.SECONDS
                )
                .addTag("analyze_meal")
                .build()

            workManager.enqueue(workRequest)

            val elapsed = System.currentTimeMillis() - screenLaunchTime
            Timber.tag(TAG).i(
                "Meal analysis enqueued: id=%s, elapsed=%dms",
                workRequest.id,
                elapsed
            )

            _state.value = CaptureState.BackgroundProcessingStarted
            processedPhotoUri = null

        } catch (e: Exception) {
            Timber.e(e, "Failed to enqueue background processing")
            _state.value = CaptureState.Error("Failed to start processing: ${e.message}")
        }
    }

    /**
     * Prepares temporary file for camera capture.
     *
     * Creates FileProvider URI and transitions to ReadyToCapture.
     */
    private fun prepareForCapture() {
        viewModelScope.launch {
            try {
                currentPhotoUri = photoManager.createPhotoFile()
                _state.value = CaptureState.ReadyToCapture(currentPhotoUri!!)
                
                // Performance logging: widget → camera ready (AC#1)
                val prepDuration = System.currentTimeMillis() - screenLaunchTime
                Timber.d("Camera ready in ${prepDuration}ms from screen launch (target: <500ms)")
                
                if (prepDuration > 500) {
                    Timber.w("Camera preparation exceeded 500ms target: ${prepDuration}ms")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to prepare for capture")
                _state.value = CaptureState.Error("Failed to prepare camera: ${e.message}")
            }
        }
    }

    /**
     * Processes captured photo with resize and compression.
     *
     * Applies 2MP max resolution and 80% JPEG quality via PhotoManager.
     * Transitions to ProcessingComplete on success or Error on failure.
     */
    private fun processPhoto() {
        viewModelScope.launch {
            try {
                val originalUri = currentPhotoUri
                    ?: throw IllegalStateException("No photo URI to process")

                processedPhotoUri = photoManager.resizeAndCompress(originalUri)

                if (processedPhotoUri != null) {
                    Timber.d("Photo processing complete: $processedPhotoUri")
                    _state.value = CaptureState.ProcessingComplete(processedPhotoUri!!)

                    // Delete original unprocessed photo
                    photoManager.deletePhoto(originalUri)
                } else {
                    throw IllegalStateException("Photo processing returned null")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to process photo")
                _state.value = CaptureState.Error("Failed to process photo: ${e.message}")
                cleanupCurrentPhoto()
            }
        }
    }

    /**
     * Cleans up current (unprocessed) photo file.
     */
    private fun cleanupCurrentPhoto() {
        viewModelScope.launch {
            currentPhotoUri?.let { uri ->
                photoManager.deletePhoto(uri)
                currentPhotoUri = null
            }
        }
    }

    /**
     * Cleans up processed photo file.
     */
    private fun cleanupProcessedPhoto() {
        viewModelScope.launch {
            processedPhotoUri?.let { uri ->
                photoManager.deletePhoto(uri)
                processedPhotoUri = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup any remaining photos on ViewModel destruction
        cleanupCurrentPhoto()
        cleanupProcessedPhoto()
    }
}
