package com.grippo.data.features.api.weight.history

import com.grippo.data.features.api.weight.history.models.WeightHistory
import kotlinx.coroutines.flow.Flow

public interface WeightHistoryFeature {

    public fun observeWeightHistory(): Flow<List<WeightHistory>>

    public fun observeLastWeight(): Flow<WeightHistory?>

    public suspend fun getWeightHistory(): Unit

    public suspend fun updateWeight(value: Float): Unit

    public suspend fun deleteWeight(id: String): Unit
}