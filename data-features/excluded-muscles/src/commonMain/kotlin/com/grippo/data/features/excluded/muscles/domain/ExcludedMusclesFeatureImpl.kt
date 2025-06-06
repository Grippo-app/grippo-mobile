package com.grippo.data.features.excluded.muscles.domain

import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.muscle.models.Muscle
import kotlinx.coroutines.flow.Flow

internal class ExcludedMusclesFeatureImpl(
    private val repository: ExcludedMusclesRepository
) : ExcludedMusclesFeature {

    override fun observeExcludedMuscles(): Flow<List<Muscle>> {
        return repository.observeExcludedMuscles()
    }

    override suspend fun getExcludedMuscles(): Result<Unit> {
        return repository.getExcludedMuscles()
    }

    override suspend fun setExcludedMuscles(ids: List<String>): Result<Unit> {
        return repository.setExcludedMuscles(ids)
    }
}