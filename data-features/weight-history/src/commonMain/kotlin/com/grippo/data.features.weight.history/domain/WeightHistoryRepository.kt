package com.grippo.data.features.weight.history.domain

import com.grippo.data.features.api.weight.history.models.WeightHistory
import kotlinx.coroutines.flow.Flow

internal interface WeightHistoryRepository {
    fun observeWeightHistory(): Flow<List<WeightHistory>>
    fun observeLastWeight(): Flow<WeightHistory?>

    suspend fun getWeightHistory(): Result<Unit>
    suspend fun updateWeight(value: Float): Result<Unit>
    suspend fun deleteWeight(id: String): Result<Unit>
}