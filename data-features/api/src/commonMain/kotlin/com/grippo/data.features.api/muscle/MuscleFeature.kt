package com.grippo.data.features.api.muscle

import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.muscle.models.MuscleGroup
import kotlinx.coroutines.flow.Flow

public interface MuscleFeature {
    public fun observeMuscles(): Flow<List<MuscleGroup>>
    public fun observeMusclesById(ids: List<String>): Flow<List<Muscle>>

    public suspend fun syncUserMuscles(): Result<Unit>
    public suspend fun syncPublicMuscles(): Result<Unit>
}