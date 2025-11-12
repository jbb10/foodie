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
 * for fetching the meal history.
 */
class GetMealHistoryUseCase @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository
) {
    operator fun invoke(): Flow<Result<List<MealEntry>>> {
        return healthConnectRepository.getMealHistory()
    }
}
