package com.grippo.data.features.api.excluded.muscles

import com.grippo.data.features.api.muscle.models.Muscle
import kotlinx.coroutines.flow.Flow

public interface ExcludedMusclesFeature {
    public fun observeExcludedMuscles(): Flow<List<Muscle>>

    public suspend fun getExcludedMuscles(): Result<Unit>
    public suspend fun setExcludedMuscle(id: String): Result<Unit>
    public suspend fun deleteExcludedMuscle(id: String): Result<Unit>
}