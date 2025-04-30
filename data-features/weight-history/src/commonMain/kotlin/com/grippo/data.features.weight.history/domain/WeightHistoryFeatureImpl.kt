package com.grippo.data.features.weight.history.domain

import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import com.grippo.data.features.api.weight.history.models.WeightHistory
import kotlinx.coroutines.flow.Flow

internal class WeightHistoryFeatureImpl(
    private val repository: WeightHistoryRepository
) : WeightHistoryFeature {

    override fun observeWeightHistory(): Flow<List<WeightHistory>> {
        return repository.observeWeightHistory()
    }

    override fun observeLastWeight(): Flow<WeightHistory?> {
        return repository.observeLastWeight()
    }

    override suspend fun getWeightHistory(): Result<Unit> {
        return repository.getWeightHistory()
    }

    override suspend fun updateWeight(value: Double): Result<Unit> {
        return repository.updateWeight(value)
    }

    override suspend fun removeWeight(id: String): Result<Unit> {
        return repository.removeWeight(id)
    }
}