package com.grippo.data.features.muscle.domain

import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single(binds = [MuscleFeature::class])
internal class MuscleFeatureImpl(
    private val repository: MuscleRepository
) : MuscleFeature {

    override fun observeMuscles(): Flow<List<MuscleGroup>> {
        return repository.observeMuscles()
    }

    override suspend fun getMuscles(): Result<Unit> {
        return repository.getMuscles()
    }
}