package com.grippo.data.features.muscle.data

import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.muscle.domain.MuscleRepository
import com.grippo.database.dao.MuscleDao
import com.grippo.database.mapper.toDomain
import com.grippo.network.Api
import com.grippo.network.mapper.toEntities
import com.grippo.network.mapper.toEntityOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class MuscleRepositoryImpl(
    private val api: Api,
    private val muscleDao: MuscleDao,
) : MuscleRepository {

    override fun observeMuscles(): Flow<List<MuscleGroup>> {
        return muscleDao.getGroups()
            .map { it.toDomain() }
    }

    override fun observeMusclesById(ids: List<String>): Flow<List<Muscle>> {
        return muscleDao.getByIds(ids)
            .map { it.toDomain() }
    }

    override suspend fun getUserMuscles(): Result<Unit> {
        val response = api.getUserMuscles()

        response.onSuccess {
            // todo save in database
        }

        return response.map { }
    }

    override suspend fun getPublicMuscles(): Result<Unit> {
        val response = api.getPublicMuscles()

        response.onSuccess { r ->
            val groups = r.mapNotNull { it.toEntityOrNull() }
            val muscles = r.mapNotNull { it.muscles?.toEntities() }.flatten()
            muscleDao.insertGroupsWithMuscles(groups, muscles)
        }

        return response.map { }
    }
}