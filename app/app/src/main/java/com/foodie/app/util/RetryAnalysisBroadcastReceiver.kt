package com.foodie.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.foodie.app.data.worker.AnalyzeMealWorker
import java.util.concurrent.TimeUnit
import timber.log.Timber

/**
 * Broadcast receiver for handling retry analysis action from notifications.
 *
 * Triggered when user taps "Retry" button in error notification.
 * Re-enqueues AnalyzeMealWorker with original input data (photo URI, timestamp).
 *
 * Architecture:
 * - BroadcastReceiver: Handles notification action outside of Activity lifecycle
 * - WorkManager: Enqueues new work request with same constraints as original
 * - Input Data: Preserves photo URI and timestamp from failed work
 *
 * Story: 4.3 - API Error Classification and Handling
 */
class RetryAnalysisBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "RetryAnalysisReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.foodie.app.RETRY_ANALYSIS") {
            Timber.tag(TAG).w("Received unexpected action: ${intent.action}")
            return
        }

        val workIdString = intent.getStringExtra("work_id")
        val photoUriString = intent.getStringExtra("photo_uri")
        val timestamp = intent.getLongExtra("timestamp", 0L)

        if (photoUriString == null || timestamp == 0L) {
            Timber.tag(TAG).e("Missing required data for retry: photoUri=$photoUriString, timestamp=$timestamp")
            return
        }

        Timber.tag(TAG).i("Retry requested for work $workIdString, photoUri=$photoUriString")

        // Verify photo URI is valid
        val photoUri = try {
            photoUriString.toUri()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Invalid photo URI: $photoUriString")
            return
        }

        // Enqueue new work request with same input data and constraints
        val workRequest = OneTimeWorkRequestBuilder<AnalyzeMealWorker>()
            .setInputData(
                workDataOf(
                    AnalyzeMealWorker.KEY_PHOTO_URI to photoUri.toString(),
                    AnalyzeMealWorker.KEY_TIMESTAMP to timestamp,
                ),
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1,
                TimeUnit.SECONDS,
            )
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        Timber.tag(TAG).i("Analysis re-enqueued: workId=${workRequest.id}, photoUri=$photoUri")
    }
}
