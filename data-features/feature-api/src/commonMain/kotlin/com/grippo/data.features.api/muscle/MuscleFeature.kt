package com.grippo.data.features.api.muscle

import com.grippo.data.features.api.muscle.models.MuscleGroup
import kotlinx.coroutines.flow.Flow

public interface MuscleFeature {

    public fun observeMuscles(): Flow<List<MuscleGroup>>

    public suspend fun getMuscles(): Result<Unit>
}