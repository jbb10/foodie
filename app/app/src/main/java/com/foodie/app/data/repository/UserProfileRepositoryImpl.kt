package com.foodie.app.data.repository

import android.content.SharedPreferences
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.domain.model.UserProfile
import com.foodie.app.domain.repository.UserProfileRepository
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Implementation of UserProfileRepository.
 *
 * Manages user demographic profile across Health Connect (weight/height) and
 * SharedPreferences (sex/birth date) storage systems.
 *
 * **Architecture:**
 * - Health Connect: Pre-populates weight/height from latest records
 * - SharedPreferences: Stores sex/birth date (keys: user_sex, user_birth_date)
 * - Repository pattern: Abstracts storage details from ViewModel
 *
 * **Data Source Priority:**
 * - Weight/Height: Health Connect WeightRecord/HeightRecord (latest)
 * - Sex/Birth Date: SharedPreferences (no Health Connect equivalent)
 *
 * @param healthConnectManager Health Connect operations wrapper
 * @param sharedPreferences Standard SharedPreferences for non-sensitive data
 */
@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val sharedPreferences: SharedPreferences,
) : UserProfileRepository {

    companion object {
        private const val TAG = "UserProfileRepository"
        private const val KEY_USER_SEX = "user_sex"
        private const val KEY_USER_BIRTH_DATE = "user_birth_date"
    }

    /**
     * Observes user profile with Health Connect pre-population.
     *
     * **Data Retrieval:**
     * 1. Reads sex and birth date from SharedPreferences
     * 2. Queries latest WeightRecord from Health Connect
     * 3. Queries latest HeightRecord from Health Connect
     * 4. Combines into UserProfile if all fields present
     *
     * **Null Handling:**
     * - Returns null if any required field missing (incomplete profile)
     * - Returns null if Health Connect permissions denied (graceful degradation)
     *
     * Note: This is a cold Flow, emits once when collected. For reactive updates when
     * Health Connect data changes externally, a more complex SharedFlow approach would be needed.
     *
     * @return Flow emitting current UserProfile or null
     */
    override fun getUserProfile(): Flow<UserProfile?> = flow {
        Timber.tag(TAG).d("Loading user profile")

        // Read sex and birth date from SharedPreferences
        val sexString = sharedPreferences.getString(KEY_USER_SEX, null)
        val birthDateString = sharedPreferences.getString(KEY_USER_BIRTH_DATE, null)
        val birthDate = birthDateString?.let { LocalDate.parse(it) }

        // Query latest weight and height from Health Connect
        val weightRecord = healthConnectManager.queryLatestWeight()
        val heightRecord = healthConnectManager.queryLatestHeight()

        // Combine if all fields present
        val profile = if (sexString != null && birthDate != null && weightRecord != null && heightRecord != null) {
            val sex = UserProfile.Sex.valueOf(sexString)
            val weightKg = weightRecord.weight.inKilograms
            val heightCm = heightRecord.height.inMeters * 100.0 // Convert meters to cm

            Timber.tag(TAG).i("Profile loaded: sex=$sex, birthDate=$birthDate, weight=$weightKg kg, height=$heightCm cm")

            UserProfile(
                sex = sex,
                birthDate = birthDate,
                weightKg = weightKg,
                heightCm = heightCm,
            )
        } else {
            Timber.tag(TAG).d("Profile incomplete: sex=$sexString, birthDate=$birthDate, weight=${weightRecord != null}, height=${heightRecord != null}")
            null
        }

        emit(profile)
    }.catch { e ->
        // Handle exceptions outside the flow builder to maintain Flow exception transparency
        Timber.tag(TAG).e(e, "Failed to load user profile")
        emit(null)
    }

    /**
     * Updates user profile with validation and selective Health Connect writes.
     *
     * **Validation:**
     * - Calls profile.validate() first
     * - Returns Result.failure with ValidationError if invalid
     *
     * **Storage Operations:**
     * 1. Save sex and birth date to SharedPreferences (always)
     * 2. If writeWeightToHC: Insert new WeightRecord with current timestamp
     * 3. If writeHeightToHC: Insert new HeightRecord with current timestamp
     *
     * **Error Handling:**
     * - ValidationError: Validation failed (age/weight/height out of range)
     * - SecurityException: Health Connect write permissions denied
     * - Other exceptions: Storage failures logged and propagated
     *
     * @param profile Complete user profile
     * @param writeWeightToHC If true, creates WeightRecord in Health Connect
     * @param writeHeightToHC If true, creates HeightRecord in Health Connect
     * @return Result.success or Result.failure with exception
     */
    override suspend fun updateProfile(
        profile: UserProfile,
        writeWeightToHC: Boolean,
        writeHeightToHC: Boolean,
    ): Result<Unit> {
        return try {
            Timber.tag(TAG).d("Updating profile: sex=${profile.sex}, birthDate=${profile.birthDate}, weight=${profile.weightKg}, height=${profile.heightCm}, writeWeight=$writeWeightToHC, writeHeight=$writeHeightToHC")

            // Validate profile
            profile.validate().getOrElse { error ->
                Timber.tag(TAG).w("Profile validation failed: ${error.message}")
                return Result.failure(error)
            }

            // Save sex and birth date to SharedPreferences
            sharedPreferences.edit()
                .putString(KEY_USER_SEX, profile.sex.name)
                .putString(KEY_USER_BIRTH_DATE, profile.birthDate.toString())
                .apply()

            Timber.tag(TAG).d("Saved sex and birth date to SharedPreferences")

            // Conditionally write weight to Health Connect
            if (writeWeightToHC) {
                val weightResult = healthConnectManager.insertWeight(
                    weightKg = profile.weightKg,
                    timestamp = Instant.now(),
                )
                weightResult.getOrElse { error ->
                    Timber.tag(TAG).e(error, "Failed to insert weight to Health Connect")
                    return Result.failure(error)
                }
                Timber.tag(TAG).d("Inserted weight to Health Connect: ${profile.weightKg} kg")
            }

            // Conditionally write height to Health Connect
            if (writeHeightToHC) {
                val heightResult = healthConnectManager.insertHeight(
                    heightCm = profile.heightCm,
                    timestamp = Instant.now(),
                )
                heightResult.getOrElse { error ->
                    Timber.tag(TAG).e(error, "Failed to insert height to Health Connect")
                    return Result.failure(error)
                }
                Timber.tag(TAG).d("Inserted height to Health Connect: ${profile.heightCm} cm")
            }

            Timber.tag(TAG).i("Profile updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to update profile")
            Result.failure(e)
        }
    }

    /**
     * Queries the latest weight record from Health Connect.
     *
     * @return Latest WeightRecord or null if none found or permissions denied
     */
    override suspend fun queryLatestWeight(): androidx.health.connect.client.records.WeightRecord? {
        return healthConnectManager.queryLatestWeight()
    }

    /**
     * Queries the latest height record from Health Connect.
     *
     * @return Latest HeightRecord or null if none found or permissions denied
     */
    override suspend fun queryLatestHeight(): androidx.health.connect.client.records.HeightRecord? {
        return healthConnectManager.queryLatestHeight()
    }
}
