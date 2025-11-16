package com.foodie.app.data.worker

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.foodie.app.R
import com.foodie.app.data.local.cache.PhotoManager
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.data.worker.foreground.MealAnalysisForegroundNotifier
import com.foodie.app.data.worker.foreground.MealAnalysisNotificationSpec
import com.foodie.app.domain.exception.NoFoodDetectedException
import com.foodie.app.domain.model.NutritionData
import com.foodie.app.domain.repository.NutritionAnalysisRepository
import com.foodie.app.util.Result as ApiResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AnalyzeMealWorkerForegroundTest {

    private lateinit var context: Context
    private lateinit var nutritionRepository: NutritionAnalysisRepository
    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var photoManager: PhotoManager
    private lateinit var foregroundNotifier: MealAnalysisForegroundNotifier
    private lateinit var networkMonitor: com.foodie.app.data.network.NetworkMonitor
    private lateinit var errorHandler: com.foodie.app.domain.error.ErrorHandler
    private lateinit var notificationHelper: com.foodie.app.util.NotificationHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        nutritionRepository = mockk()
        healthConnectManager = mockk()
        photoManager = mockk()
        foregroundNotifier = mockk()
        networkMonitor = mockk()
        errorHandler = mockk()
        notificationHelper = mockk()

        val notification = NotificationCompat.Builder(context, MealAnalysisNotificationSpec.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Analyzing meal…")
            .setContentText("Preparing meal analysis…")
            .build()

        every { foregroundNotifier.createForegroundInfo(any(), any(), any()) } returns
            androidx.work.ForegroundInfo(MealAnalysisNotificationSpec.ONGOING_NOTIFICATION_ID, notification)
        every { foregroundNotifier.createCompletionNotification(any(), any(), any()) } returns notification
        every { foregroundNotifier.createFailureNotification(any(), any()) } returns notification
        every { networkMonitor.checkConnectivity() } returns true

        runBlocking {
            coEvery { nutritionRepository.analyzePhoto(any()) } returns
                ApiResult.Success(NutritionData(calories = 480, description = "Veggie bowl"))
            coEvery { healthConnectManager.insertNutritionRecord(any(), any(), any()) } returns "record-123"
            coEvery { photoManager.deletePhoto(any()) } returns true
        }
    }

    @Test
    fun doWork_requestsForegroundAndCompletesSuccessfully() = runBlocking {
        val inputData = androidx.work.workDataOf(
            AnalyzeMealWorker.KEY_PHOTO_URI to "content://foodie/photos/123",
            AnalyzeMealWorker.KEY_TIMESTAMP to java.time.Instant.now().epochSecond
        )

        val worker = TestListenableWorkerBuilder<AnalyzeMealWorker>(context)
            .setInputData(inputData)
            .setWorkerFactory(testWorkerFactory())
            .build()

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        verify(atLeast = 1) { foregroundNotifier.createForegroundInfo(worker.id, any(), any()) }
        verify { foregroundNotifier.createCompletionNotification(any(), any(), any()) }
        coVerify(atLeast = 1) { photoManager.deletePhoto(any()) }
    }

    @Test
    fun doWork_nonRetryableErrorDeletesPhotoAndReportsFailure() = runBlocking {
        coEvery { nutritionRepository.analyzePhoto(any()) } returns
            ApiResult.Error(IllegalArgumentException("Invalid payload"))

        val inputData = androidx.work.workDataOf(
            AnalyzeMealWorker.KEY_PHOTO_URI to "content://foodie/photos/456",
            AnalyzeMealWorker.KEY_TIMESTAMP to java.time.Instant.now().epochSecond
        )

        val worker = TestListenableWorkerBuilder<AnalyzeMealWorker>(context)
            .setInputData(inputData)
            .setWorkerFactory(testWorkerFactory())
            .build()

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        coVerify(atLeast = 1) { photoManager.deletePhoto(any()) }
        verify(atLeast = 1) { foregroundNotifier.createForegroundInfo(worker.id, any(), any()) }
        verify { foregroundNotifier.createFailureNotification(worker.id, any()) }
        verify(exactly = 0) { foregroundNotifier.createCompletionNotification(any(), any(), any()) }
    }

    @Test
    fun doWork_maxRetriesExceededDeletesPhotoAndFails() = runBlocking {
        coEvery { nutritionRepository.analyzePhoto(any()) } returns
            ApiResult.Error(IOException("Timeout"))

        val inputData = androidx.work.workDataOf(
            AnalyzeMealWorker.KEY_PHOTO_URI to "content://foodie/photos/789",
            AnalyzeMealWorker.KEY_TIMESTAMP to java.time.Instant.now().epochSecond
        )

        val worker = TestListenableWorkerBuilder<AnalyzeMealWorker>(context)
            .setInputData(inputData)
            .setWorkerFactory(testWorkerFactory())
            .setRunAttemptCount(3)
            .build()

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        coVerify(atLeast = 1) { photoManager.deletePhoto(any()) }
        verify { foregroundNotifier.createFailureNotification(worker.id, any()) }
    }

    @Test
    fun doWork_whenNoFoodDetected_deletesPhotoAndShowsSpecificMessage() = runBlocking {
        coEvery { nutritionRepository.analyzePhoto(any()) } returns
            ApiResult.Error(
                exception = NoFoodDetectedException("Image shows a document, not food"),
                message = "Image shows a document, not food"
            )

        val inputData = androidx.work.workDataOf(
            AnalyzeMealWorker.KEY_PHOTO_URI to "content://foodie/photos/document.jpg",
            AnalyzeMealWorker.KEY_TIMESTAMP to java.time.Instant.now().epochSecond
        )

        val worker = TestListenableWorkerBuilder<AnalyzeMealWorker>(context)
            .setInputData(inputData)
            .setWorkerFactory(testWorkerFactory())
            .build()

        val result = worker.doWork()

        // Should fail immediately (non-retryable)
        assertTrue(result is ListenableWorker.Result.Failure)
        
        // Photo should be deleted
        coVerify(atLeast = 1) { photoManager.deletePhoto(any()) }
        
        // Should show failure notification with user-friendly message
        verify { foregroundNotifier.createFailureNotification(worker.id, "No food detected. Please take a photo of your meal.") }
        
        // Should NOT retry (even on first attempt)
        verify(exactly = 0) { foregroundNotifier.createCompletionNotification(any(), any(), any()) }
    }

    private fun testWorkerFactory(): WorkerFactory = object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return AnalyzeMealWorker(
                appContext,
                workerParameters,
                nutritionRepository,
                healthConnectManager,
                photoManager,
                foregroundNotifier,
                networkMonitor,
                errorHandler,
                notificationHelper
            )
        }
    }
}
