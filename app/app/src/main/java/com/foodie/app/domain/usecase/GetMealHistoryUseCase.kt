package com.foodie.app.domain.usecase

import com.foodie.app.data.repository.HealthConnectRepository
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get the meal history from the last 7 days.
 *
 * This use case abstracts the data source from the ViewModel and provides a clean API
 * for fetching the meal history. Delegates to HealthConnectRepository which handles
 * querying Health Connect and mapping to domain models.
 *
 * @param healthConnectRepository Repository for Health Connect operations
 */
class GetMealHistoryUseCase @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository
) {
    /**
     * Invokes the use case to get meal history.
     *
     * @return Flow emitting Result with list of MealEntry from last 7 days, sorted newest first
     */
    operator fun invoke(): Flow<Result<List<MealEntry>>> {
        return healthConnectRepository.getMealHistory()
    }
}
