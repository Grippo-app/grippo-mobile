package com.grippo.data.features.excluded.muscles.domain

import com.grippo.data.features.api.muscle.models.Muscle
import kotlinx.coroutines.flow.Flow

internal interface ExcludedMusclesRepository {

    fun observeExcludedMuscles(): Flow<List<Muscle>>

    suspend fun getExcludedMuscles(): Result<Unit>

    suspend fun setExcludedMuscles(ids: List<String>): Result<Unit>
}