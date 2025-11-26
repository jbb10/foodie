package com.foodie.app.data.healthconnect

import android.os.RemoteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.data.repository.HealthConnectRepository
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.util.Result
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests validating the Health Connect update flow using the real SDK.
 *
 * These tests exercise the delete + insert pattern implemented in [HealthConnectRepository]
 * to ensure records are replaced correctly, timestamps remain intact, and refreshed queries
 * surface the updated data that powers the meal list.
 *
 * All tests skip gracefully when Health Connect is unavailable or permissions are missing.
 */
@RunWith(AndroidJUnit4::class)
class HealthConnectUpdateIntegrationTest {

    @Test
    fun updateNutritionRecord_replacesRecordWithUpdatedValues() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = HealthConnectManager(context)

        if (!manager.isAvailable()) {
            return@runTest
        }

        val repository = HealthConnectRepository(manager)
        val timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val originalDescription = "Update Flow Original ${System.currentTimeMillis()}"
        val updatedDescription = "$originalDescription - Updated"
        val originalCalories = 640
        val updatedCalories = 720

        var cleanupRecordId: String? = null

        try {
            val originalId = manager.insertNutritionRecord(
                calories = originalCalories,
                description = originalDescription,
                timestamp = timestamp,
            )

            val updateResult = repository.updateNutritionRecord(
                recordId = originalId,
                calories = updatedCalories,
                description = updatedDescription,
                timestamp = timestamp,
            )

            assertThat(updateResult).isInstanceOf(Result.Success::class.java)

            val records = manager.queryNutritionRecords(
                startTime = timestamp.minusSeconds(60),
                endTime = timestamp.plusSeconds(60),
            )

            val oldRecordStillExists = records.any { it.metadata.id == originalId }
            assertThat(oldRecordStillExists).isFalse()

            val updatedRecord = records.find { record ->
                record.name == updatedDescription &&
                    record.energy?.inKilocalories == updatedCalories.toDouble()
            }

            assertThat(updatedRecord).isNotNull()
            cleanupRecordId = updatedRecord!!.metadata.id
        } catch (security: SecurityException) {
            return@runTest
        } catch (unavailable: IllegalStateException) {
            return@runTest
        } catch (remote: RemoteException) {
            return@runTest
        } finally {
            cleanupRecordId?.let { manager.deleteNutritionRecord(it) }
        }
    }

    @Test
    fun updateNutritionRecord_preservesOriginalTimestamp() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = HealthConnectManager(context)

        if (!manager.isAvailable()) {
            return@runTest
        }

        val repository = HealthConnectRepository(manager)
        val timestamp = Instant.now().minusSeconds(90).truncatedTo(ChronoUnit.SECONDS)
        val originalDescription = "Update Flow Timestamp ${System.currentTimeMillis()}"
        val updatedDescription = "$originalDescription - Updated"
        val calories = 540
        val updatedCalories = 815
        val expectedZoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)

        var cleanupRecordId: String? = null

        try {
            val originalId = manager.insertNutritionRecord(
                calories = calories,
                description = originalDescription,
                timestamp = timestamp,
            )

            val updateResult = repository.updateNutritionRecord(
                recordId = originalId,
                calories = updatedCalories,
                description = updatedDescription,
                timestamp = timestamp,
            )

            assertThat(updateResult).isInstanceOf(Result.Success::class.java)

            val records = manager.queryNutritionRecords(
                startTime = timestamp.minusSeconds(60),
                endTime = timestamp.plusSeconds(60),
            )

            val updatedRecord = records.find { it.name == updatedDescription }
            assertThat(updatedRecord).isNotNull()

            cleanupRecordId = updatedRecord!!.metadata.id
            assertThat(updatedRecord.startTime).isEqualTo(timestamp)
            assertThat(updatedRecord.endTime).isEqualTo(timestamp.plusSeconds(1))
            assertThat(updatedRecord.startZoneOffset).isEqualTo(expectedZoneOffset)
            assertThat(updatedRecord.endZoneOffset).isEqualTo(expectedZoneOffset)
        } catch (security: SecurityException) {
            return@runTest
        } catch (unavailable: IllegalStateException) {
            return@runTest
        } catch (remote: RemoteException) {
            return@runTest
        } finally {
            cleanupRecordId?.let { manager.deleteNutritionRecord(it) }
        }
    }

    @Test
    fun updateNutritionRecord_refreshesMealHistoryWithUpdatedEntry() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = HealthConnectManager(context)

        if (!manager.isAvailable()) {
            return@runTest
        }

        val repository = HealthConnectRepository(manager)
        val timestamp = Instant.now().minusSeconds(120).truncatedTo(ChronoUnit.SECONDS)
        val originalDescription = "Update Flow History ${System.currentTimeMillis()}"
        val updatedDescription = "$originalDescription - Updated"
        val originalCalories = 480
        val updatedCalories = 910

        var cleanupRecordId: String? = null

        try {
            val originalId = manager.insertNutritionRecord(
                calories = originalCalories,
                description = originalDescription,
                timestamp = timestamp,
            )

            val updateResult = repository.updateNutritionRecord(
                recordId = originalId,
                calories = updatedCalories,
                description = updatedDescription,
                timestamp = timestamp,
            )

            assertThat(updateResult).isInstanceOf(Result.Success::class.java)

            val historyResult = repository.getMealHistory()
                .first { result -> result is Result.Success<*> }

            val success = historyResult as Result.Success<List<MealEntry>>
            val updatedMeal = success.data.firstOrNull { meal ->
                meal.description == updatedDescription && meal.calories == updatedCalories
            }

            assertThat(updatedMeal).isNotNull()
            cleanupRecordId = updatedMeal!!.id
            assertThat(updatedMeal.timestamp).isEqualTo(timestamp)
        } catch (security: SecurityException) {
            return@runTest
        } catch (unavailable: IllegalStateException) {
            return@runTest
        } catch (remote: RemoteException) {
            return@runTest
        } finally {
            cleanupRecordId?.let { manager.deleteNutritionRecord(it) }
        }
    }
}
