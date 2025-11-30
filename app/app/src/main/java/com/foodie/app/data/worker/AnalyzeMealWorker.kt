package com.foodie.app.data.worker

import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.foodie.app.R
import com.foodie.app.data.local.cache.PhotoManager
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.data.network.NetworkMonitor
import com.foodie.app.data.worker.foreground.MealAnalysisForegroundNotifier
import com.foodie.app.data.worker.foreground.MealAnalysisNotificationSpec
import com.foodie.app.domain.error.ErrorHandler
import com.foodie.app.domain.error.ErrorType
import com.foodie.app.domain.exception.NoFoodDetectedException
import com.foodie.app.domain.model.NutritionData
import com.foodie.app.domain.repository.NutritionAnalysisRepository
import com.foodie.app.util.Result as ApiResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import timber.log.Timber

/**
 * Background worker for analyzing meal photos and saving nutrition data to Health Connect.
 *
 * Orchestrates the complete processing flow:
 * 1. Read photo URI and timestamp from input data
 * 2. Call Azure OpenAI API via NutritionAnalysisRepository
 * 3. Save analyzed nutrition data to Health Connect
 * 4. Delete temporary photo file
 * 5. Handle errors with retry logic (exponential backoff)
 *
 * Retry Strategy:
 * - Retryable errors (network failures, timeouts, 5xx): Return Result.retry()
 * - Non-retryable errors (4xx, parse errors, validation): Return Result.failure() immediately
 * - Max 4 attempts total (initial + 3 retries with 1s, 2s, 4s delays)
 * - Photo cleanup: Delete after success, max retries, or non-retryable error
 * - Photo kept only if Health Connect SecurityException (permission denied)
 *
 * Performance Targets:
 * - < 15 seconds typical processing time (API call + Health Connect save)
 * - < 30 seconds 95th percentile (including slow network)
 *
 * Architecture:
 * - @HiltWorker enables constructor dependency injection
 * - CoroutineWorker provides suspend function support
 * - WorkManager ensures reliability (survives app termination, doze mode)
 * - Network constraint: Only runs when NetworkType.CONNECTED
 *
 * Usage:
 * ```
 * val workRequest = OneTimeWorkRequestBuilder<AnalyzeMealWorker>()
 *     .setInputData(workDataOf(
 *         AnalyzeMealWorker.KEY_PHOTO_URI to photoUri.toString(),
 *         AnalyzeMealWorker.KEY_TIMESTAMP to Instant.now().epochSecond
 *     ))
 *     .setConstraints(Constraints.Builder()
 *         .setRequiredNetworkType(NetworkType.CONNECTED)
 *         .build())
 *     .setBackoffCriteria(
 *         BackoffPolicy.EXPONENTIAL,
 *         1, TimeUnit.SECONDS
 *     )
 *     .build()
 * workManager.enqueue(workRequest)
 * ```
 */
@HiltWorker
class AnalyzeMealWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val nutritionAnalysisRepository: NutritionAnalysisRepository,
    private val healthConnectManager: HealthConnectManager,
    private val photoManager: PhotoManager,
    private val foregroundNotifier: MealAnalysisForegroundNotifier,
    private val networkMonitor: NetworkMonitor,
    private val errorHandler: ErrorHandler,
    private val notificationHelper: com.foodie.app.util.NotificationHelper,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AnalyzeMealWorker"

        /**
         * Input data key for photo URI (String).
         */
        const val KEY_PHOTO_URI = "photo_uri"

        /**
         * Input data key for meal timestamp in epoch seconds (Long).
         */
        const val KEY_TIMESTAMP = "timestamp"

        /**
         * Maximum number of processing attempts (initial + retries).
         * Attempts: 1 (immediate), 2 (1s delay), 3 (2s delay), 4 (4s delay)
         */
        private const val MAX_ATTEMPTS = 4
    }

    private val notificationManager by lazy { NotificationManagerCompat.from(appContext) }

    @SuppressLint("NewApi")
    private suspend fun startForeground(statusTextRes: Int): androidx.work.ListenableWorker.Result? {
        val statusText = appContext.getString(statusTextRes)
        return try {
            setForeground(foregroundNotifier.createForegroundInfo(id, statusText))
            null
        } catch (e: ForegroundServiceStartNotAllowedException) {
            Timber.tag(TAG).e(e, "Foreground service start blocked; scheduling retry")
            androidx.work.ListenableWorker.Result.retry()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to enter foreground state")
            androidx.work.ListenableWorker.Result.retry()
        }
    }

    private suspend fun updateForeground(statusTextRes: Int, progressPercent: Int? = null) {
        val statusText = appContext.getString(statusTextRes)
        try {
            setForeground(foregroundNotifier.createForegroundInfo(id, statusText, progressPercent))
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Unable to update foreground notification")
        }
    }

    @SuppressLint("MissingPermission")
    private fun notifySuccess(data: NutritionData, recordId: String, timestamp: Instant) {
        notificationManager.notify(
            MealAnalysisNotificationSpec.COMPLETION_NOTIFICATION_ID,
            foregroundNotifier.createCompletionNotification(data, recordId, timestamp),
        )
    }

    @SuppressLint("MissingPermission")
    private fun notifyFailure(message: String?) {
        val fallback = appContext.getString(R.string.notification_meal_analysis_failure_generic)
        val displayMessage = message?.takeIf { it.isNotBlank() } ?: fallback

        notificationManager.notify(
            MealAnalysisNotificationSpec.COMPLETION_NOTIFICATION_ID,
            foregroundNotifier.createFailureNotification(id, displayMessage),
        )
    }

    /**
     * Performs background processing of a meal photo.
     *
     * Flow:
     * 1. Extract photo URI and timestamp from input data
     * 2. Verify photo file exists (return failure if missing)
     * 3. Call API for nutrition analysis
     * 4. On success: Save to Health Connect → Delete photo → Return success
     * 5. On retryable error: Return retry (if attempts < MAX_ATTEMPTS), else delete photo and fail
     * 6. On non-retryable error: Delete photo → Return failure
     * 7. On SecurityException: Keep photo → Return failure (manual intervention needed)
     *
     * @return Result.success() if processing completes successfully
     *         Result.retry() if retryable error and attempts < MAX_ATTEMPTS
     *         Result.failure() if non-retryable error or max attempts exhausted
     */
    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val startTime = System.currentTimeMillis()

        // Extract and validate input data
        val inputValidation = validateInputData()
        if (inputValidation != null) return inputValidation

        val photoUriString = inputData.getString(KEY_PHOTO_URI)!!
        val timestampSeconds = inputData.getLong(KEY_TIMESTAMP, 0L)
        val photoUri = photoUriString.toUri()
        val timestamp = Instant.ofEpochSecond(timestampSeconds)

        Timber.tag(TAG).d(
            "Starting meal analysis (attempt ${runAttemptCount + 1}/$MAX_ATTEMPTS): " +
                "uri=$photoUri, timestamp=$timestamp",
        )

        startForeground(R.string.notification_meal_analysis_status_preparing)?.let { return it }

        // Check network connectivity
        val networkCheck = checkNetworkConnectivity()
        if (networkCheck != null) return networkCheck

        updateForeground(R.string.notification_meal_analysis_status_uploading, progressPercent = 10)

        return try {
            processAnalysis(photoUri, timestamp, startTime)
        } catch (e: Exception) {
            handleUnexpectedException(e, photoUri)
        }
    }

    private fun validateInputData(): androidx.work.ListenableWorker.Result? {
        val photoUriString = inputData.getString(KEY_PHOTO_URI)

        if (photoUriString == null) {
            Timber.tag(TAG).e("Photo URI missing from input data")
            notifyFailure("Photo URI missing from input data")
            return androidx.work.ListenableWorker.Result.failure()
        }

        return null
    }

    private suspend fun checkNetworkConnectivity(): androidx.work.ListenableWorker.Result? {
        if (!networkMonitor.checkConnectivity()) {
            Timber.tag(TAG).w(
                "No network connectivity (attempt ${runAttemptCount + 1}/$MAX_ATTEMPTS)",
            )

            return if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
                Timber.tag(TAG).e(
                    "Max retry attempts exhausted with no network. " +
                        "Keeping photo for future retry",
                )
                notifyFailure("No internet connection. Please check your network and try again.")
                androidx.work.ListenableWorker.Result.failure()
            } else {
                updateForeground(R.string.notification_meal_analysis_status_preparing)
                androidx.work.ListenableWorker.Result.retry()
            }
        }

        return null
    }

    private suspend fun processAnalysis(
        photoUri: android.net.Uri,
        timestamp: Instant,
        startTime: Long,
    ): androidx.work.ListenableWorker.Result {
        updateForeground(R.string.notification_meal_analysis_status_calling_api, progressPercent = 30)
        val apiStartTime = System.currentTimeMillis()
        val apiResult = nutritionAnalysisRepository.analyzePhoto(photoUri)
        val apiDuration = System.currentTimeMillis() - apiStartTime

        Timber.tag(TAG).d("API call completed in ${apiDuration}ms")

        return when (apiResult) {
            is ApiResult.Success -> handleSuccessResult(apiResult.data, photoUri, timestamp, startTime, apiDuration)
            is ApiResult.Error -> handleErrorResult(apiResult, photoUri, timestamp)
            is ApiResult.Loading -> handleUnexpectedLoadingState()
        }
    }

    private suspend fun handleSuccessResult(
        nutritionData: NutritionData,
        photoUri: android.net.Uri,
        timestamp: Instant,
        startTime: Long,
        apiDuration: Long,
    ): androidx.work.ListenableWorker.Result {
        Timber.tag(TAG).i(
            "API analysis successful: ${nutritionData.calories} kcal, " +
                "'${nutritionData.description}'",
        )

        updateForeground(R.string.notification_meal_analysis_status_saving, progressPercent = 80)
        val saveStartTime = System.currentTimeMillis()

        return try {
            if (!healthConnectManager.isAvailable()) {
                Timber.tag(TAG).e("Health Connect not available - cannot save nutrition data")
                notificationHelper.showErrorNotification(ErrorType.HealthConnectUnavailable)
                return androidx.work.ListenableWorker.Result.failure()
            }

            val recordId = healthConnectManager.insertNutritionRecord(
                calories = nutritionData.calories,
                description = nutritionData.description,
                timestamp = timestamp,
                protein = nutritionData.protein,
                carbs = nutritionData.carbs,
                fat = nutritionData.fat,
            )
            val saveDuration = System.currentTimeMillis() - saveStartTime

            Timber.tag(TAG).i(
                "Health Connect save successful in ${saveDuration}ms: recordId=$recordId",
            )

            cleanupAfterSuccess(photoUri, startTime, apiDuration, saveDuration, nutritionData, recordId, timestamp)
        } catch (e: IllegalStateException) {
            handleHealthConnectUnavailable(e)
        } catch (e: SecurityException) {
            handlePermissionDenied(e)
        } catch (e: IllegalArgumentException) {
            handleInvalidData(e, photoUri)
        }
    }

    private suspend fun cleanupAfterSuccess(
        photoUri: android.net.Uri,
        startTime: Long,
        apiDuration: Long,
        saveDuration: Long,
        nutritionData: NutritionData,
        recordId: String,
        timestamp: Instant,
    ): androidx.work.ListenableWorker.Result {
        val deleted = photoManager.deletePhoto(photoUri)
        if (deleted) {
            Timber.tag(TAG).d("Photo deleted successfully")
        } else {
            Timber.tag(TAG).w("Failed to delete photo (may have been already deleted)")
        }

        val totalDuration = System.currentTimeMillis() - startTime
        Timber.tag(TAG).i(
            "Processing completed successfully in ${totalDuration}ms " +
                "(API: ${apiDuration}ms, Save: ${saveDuration}ms)",
        )

        if (totalDuration > 20_000) {
            Timber.tag(TAG).w(
                "Slow processing detected: ${totalDuration}ms (target: <15s typical)",
            )
        }

        notifySuccess(nutritionData, recordId, timestamp)
        return androidx.work.ListenableWorker.Result.success()
    }

    private fun handleHealthConnectUnavailable(e: IllegalStateException): androidx.work.ListenableWorker.Result {
        Timber.tag(TAG).e(e, "Health Connect unavailable - keeping photo for retry after HC installation")
        notificationHelper.showErrorNotification(ErrorType.HealthConnectUnavailable)
        return androidx.work.ListenableWorker.Result.failure()
    }

    private fun handlePermissionDenied(e: SecurityException): androidx.work.ListenableWorker.Result {
        Timber.tag(TAG).e(
            e,
            "Health Connect permission denied - keeping photo for manual intervention",
        )
        notifyFailure(e.message)
        return androidx.work.ListenableWorker.Result.failure()
    }

    private fun handleInvalidData(e: IllegalArgumentException, photoUri: android.net.Uri): androidx.work.ListenableWorker.Result {
        Timber.tag(TAG).e(
            e,
            "Invalid nutrition data from API - keeping photo for investigation. " +
                "Photo: $photoUri",
        )
        notifyFailure(e.message)
        return androidx.work.ListenableWorker.Result.failure()
    }

    private suspend fun handleErrorResult(
        apiResult: ApiResult.Error,
        photoUri: android.net.Uri,
        timestamp: Instant,
    ): androidx.work.ListenableWorker.Result {
        val exception = apiResult.exception

        // Check for NoFoodDetectedException - non-retryable
        if (exception is NoFoodDetectedException) {
            Timber.tag(TAG).w("No food detected in image: ${apiResult.message}")
            photoManager.deletePhoto(photoUri)
            notifyFailure("No food detected. Please take a photo of your meal.")
            return androidx.work.ListenableWorker.Result.failure()
        }

        // Classify error using ErrorHandler
        val errorType = errorHandler.classify(exception)
        val isRetryable = errorHandler.isRetryable(errorType)
        val userMessage = errorHandler.getUserMessage(errorType)

        Timber.tag(TAG).e(
            exception,
            "API error (attempt ${runAttemptCount + 1}/$MAX_ATTEMPTS): " +
                "errorType=${errorType.javaClass.simpleName}, retryable=$isRetryable, " +
                "photoUri=$photoUri, timestamp=$timestamp",
        )

        return if (isRetryable) {
            handleRetryableError(exception, errorType, userMessage, photoUri)
        } else {
            handleNonRetryableError(exception, errorType, userMessage, photoUri)
        }
    }

    private fun handleRetryableError(
        exception: Throwable,
        errorType: ErrorType,
        userMessage: String,
        photoUri: android.net.Uri,
    ): androidx.work.ListenableWorker.Result {
        return if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
            Timber.tag(TAG).e(
                exception,
                "Max retry attempts exhausted ($MAX_ATTEMPTS), keeping photo for manual retry. " +
                    "ErrorType=${errorType.javaClass.simpleName}, photoUri=$photoUri",
            )
            notifyFailure(userMessage)
            androidx.work.ListenableWorker.Result.failure()
        } else {
            Timber.tag(TAG).w(
                exception,
                "Retryable error (attempt ${runAttemptCount + 1}/$MAX_ATTEMPTS): $userMessage. " +
                    "Will retry with backoff.",
            )
            androidx.work.ListenableWorker.Result.retry()
        }
    }

    private fun handleNonRetryableError(
        exception: Throwable,
        errorType: ErrorType,
        userMessage: String,
        photoUri: android.net.Uri,
    ): androidx.work.ListenableWorker.Result {
        Timber.tag(TAG).e(
            exception,
            "Non-retryable error: ${errorType.javaClass.simpleName}, " +
                "message='$userMessage', photoUri=$photoUri. Keeping photo for manual retry.",
        )

        when (errorType) {
            is ErrorType.AuthError -> {
                notificationHelper.showErrorNotification(errorType)
            }
            is ErrorType.PermissionDenied -> {
                notificationHelper.showErrorNotification(errorType)
            }
            else -> {
                notifyFailure(userMessage)
            }
        }

        return androidx.work.ListenableWorker.Result.failure()
    }

    private fun handleUnexpectedLoadingState(): androidx.work.ListenableWorker.Result {
        Timber.tag(TAG).e("Unexpected Loading state from repository")
        notifyFailure("Unexpected repository state")
        return androidx.work.ListenableWorker.Result.failure()
    }

    private fun handleUnexpectedException(e: Exception, photoUri: android.net.Uri): androidx.work.ListenableWorker.Result {
        Timber.tag(TAG).e(e, "Unexpected exception in worker, keeping photo for investigation: $photoUri")
        notifyFailure(e.message)
        return androidx.work.ListenableWorker.Result.failure()
    }
}
