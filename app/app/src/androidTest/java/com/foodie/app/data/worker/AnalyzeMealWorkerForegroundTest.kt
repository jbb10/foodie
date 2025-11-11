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
import com.foodie.app.domain.model.NutritionData
import com.foodie.app.domain.repository.NutritionAnalysisRepository
import com.foodie.app.util.Result as ApiResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AnalyzeMealWorkerForegroundTest {

    private lateinit var context: Context
    private lateinit var nutritionRepository: NutritionAnalysisRepository
    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var photoManager: PhotoManager
    private lateinit var foregroundNotifier: MealAnalysisForegroundNotifier

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        nutritionRepository = mock()
        healthConnectManager = mock()
        photoManager = mock()
        foregroundNotifier = mock()

        val notification = NotificationCompat.Builder(context, MealAnalysisNotificationSpec.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Analyzing meal…")
            .setContentText("Preparing meal analysis…")
            .build()

        whenever(foregroundNotifier.createForegroundInfo(any(), any(), anyOrNull())).thenReturn(
            androidx.work.ForegroundInfo(MealAnalysisNotificationSpec.ONGOING_NOTIFICATION_ID, notification)
        )
        whenever(foregroundNotifier.createCompletionNotification(any())).thenReturn(notification)
        whenever(foregroundNotifier.createFailureNotification(any(), any())).thenReturn(notification)

        whenever(nutritionRepository.analyzePhoto(any())).thenReturn(
            ApiResult.Success(NutritionData(calories = 480, description = "Veggie bowl"))
        )
        whenever(healthConnectManager.insertNutritionRecord(any(), any(), any())).thenReturn("record-123")
        whenever(photoManager.deletePhoto(any())).thenReturn(true)
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
        verify(foregroundNotifier, atLeastOnce()).createForegroundInfo(eq(worker.id), any(), anyOrNull())
        verify(foregroundNotifier).createCompletionNotification(any())
        verify(photoManager, atLeastOnce()).deletePhoto(any())
    }

    @Test
    fun doWork_nonRetryableErrorDeletesPhotoAndReportsFailure() = runBlocking {
        whenever(nutritionRepository.analyzePhoto(any())).thenReturn(
            ApiResult.Error(IllegalArgumentException("Invalid payload"))
        )

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
        verify(photoManager, atLeastOnce()).deletePhoto(any())
        verify(foregroundNotifier, atLeastOnce()).createForegroundInfo(eq(worker.id), any(), anyOrNull())
        verify(foregroundNotifier).createFailureNotification(eq(worker.id), any())
        verify(foregroundNotifier, never()).createCompletionNotification(any())
    }

    @Test
    fun doWork_maxRetriesExceededDeletesPhotoAndFails() = runBlocking {
        whenever(nutritionRepository.analyzePhoto(any())).thenReturn(
            ApiResult.Error(IOException("Timeout"))
        )

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
        verify(photoManager, atLeastOnce()).deletePhoto(any())
        verify(foregroundNotifier).createFailureNotification(eq(worker.id), any())
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
                foregroundNotifier
            )
        }
    }
}
