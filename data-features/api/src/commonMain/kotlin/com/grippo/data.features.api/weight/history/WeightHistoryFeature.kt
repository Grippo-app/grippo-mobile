package com.grippo.data.features.api.weight.history

import com.grippo.data.features.api.user.models.WeightHistory
import kotlinx.coroutines.flow.Flow

public interface WeightHistoryFeature {
    public fun observeWeightHistory(): Flow<List<WeightHistory>>
    public fun observeLastWeight(): Flow<WeightHistory>

    public suspend fun getWeightHistory(): Result<Unit>
    public suspend fun updateWeight(value: Double): Result<Unit>
    public suspend fun removeWeight(id: String): Result<Unit>
}