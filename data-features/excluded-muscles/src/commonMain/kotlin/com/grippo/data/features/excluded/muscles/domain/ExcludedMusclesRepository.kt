package com.grippo.data.features.excluded.muscles.domain

import com.grippo.data.features.api.muscle.models.Muscle
import kotlinx.coroutines.flow.Flow

internal interface ExcludedMusclesRepository {
    fun observeExcludedMuscles(): Flow<List<Muscle>>

    suspend fun getExcludedMuscles(): Result<Unit>
    suspend fun setExcludedMuscle(id: String): Result<Unit>
    suspend fun deleteExcludedMuscle(id: String): Result<Unit>
}