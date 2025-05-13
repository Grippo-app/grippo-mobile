package com.grippo.data.features.muscle.domain

import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.muscle.models.MuscleGroup
import kotlinx.coroutines.flow.Flow

internal interface MuscleRepository {
    fun observeMuscles(): Flow<List<MuscleGroup>>
    fun observeMusclesById(ids: List<String>): Flow<List<Muscle>>

    suspend fun getMuscles(): Result<Unit>
}