package com.grippo.data.features.muscle.domain

import com.grippo.data.features.api.muscle.models.MuscleGroup
import kotlinx.coroutines.flow.Flow

internal interface MuscleRepository {

    fun observeMuscles(): Flow<List<MuscleGroup>>

    suspend fun getMuscles(): Result<Unit>
}