package com.foodie.app.domain.repository

import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import com.foodie.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository for user demographic profile management.
 *
 * Manages user profile data for BMR (Basal Metabolic Rate) calculation across two storage systems:
 * - Health Connect: Weight and height (pre-population and sync)
 * - SharedPreferences: Sex and age (not available in Health Connect)
 *
 * **Data Flow:**
 * - READ: Queries latest weight/height from Health Connect, sex/age from SharedPreferences
 * - WRITE: Saves sex/age to SharedPreferences, optionally writes weight/height to Health Connect
 *
 * **Pre-population Strategy:**
 * getUserProfile() reads from Health Connect to pre-fill weight/height when available,
 * reducing manual entry burden for users who track these metrics in other apps.
 *
 * **Selective Health Connect Writes:**
 * updateProfile() only writes weight/height to Health Connect when user explicitly edits those
 * fields, avoiding pollution with redundant entries when user already tracks via other apps.
 *
 * @see UserProfile for validation ranges and BMR equation requirements
 */
interface UserProfileRepository {
    /**
     * Observes the user profile with reactive updates.
     *
     * Combines data from Health Connect (weight, height) and SharedPreferences (sex, age).
     * Emits null if profile is not configured (any required field missing).
     *
     * **Pre-population Behavior:**
     * - Weight/height auto-populated from Health Connect latest records
     * - Sex/age remain null until user first configures profile
     *
     * **Use Cases:**
     * - ViewModel init{} to pre-populate settings UI
     * - BMR calculator to retrieve profile for calculations
     *
     * @return Flow emitting current UserProfile or null if not configured
     */
    fun getUserProfile(): Flow<UserProfile?>

    /**
     * Updates the complete user profile with validation.
     *
     * **Storage Behavior:**
     * - Sex & age: Always saved to SharedPreferences
     * - Weight & height: Written to Health Connect ONLY if explicitly edited by user
     *   (controlled by writeWeightToHC and writeHeightToHC flags)
     *
     * **Validation:**
     * - Calls profile.validate() before save
     * - Returns Result.failure with ValidationError if validation fails
     * - Returns Result.failure with SecurityException if Health Connect permissions denied
     *
     * **Timestamp:**
     * - Uses Instant.now() for new Health Connect records
     *
     * @param profile Complete user profile with all fields
     * @param writeWeightToHC If true, creates new WeightRecord in Health Connect
     * @param writeHeightToHC If true, creates new HeightRecord in Health Connect
     * @return Result.success if saved, Result.failure with exception if validation or save fails
     */
    suspend fun updateProfile(
        profile: UserProfile,
        writeWeightToHC: Boolean = true,
        writeHeightToHC: Boolean = true
    ): Result<Unit>

    /**
     * Queries the latest weight record from Health Connect.
     *
     * Used for pre-populating weight field when profile is incomplete.
     *
     * @return Latest WeightRecord or null if none found or permissions denied
     */
    suspend fun queryLatestWeight(): WeightRecord?

    /**
     * Queries the latest height record from Health Connect.
     *
     * Used for pre-populating height field when profile is incomplete.
     *
     * @return Latest HeightRecord or null if none found or permissions denied
     */
    suspend fun queryLatestHeight(): HeightRecord?
}
