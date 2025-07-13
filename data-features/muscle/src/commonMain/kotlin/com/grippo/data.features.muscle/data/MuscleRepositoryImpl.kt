package com.grippo.data.features.muscle.data

import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.muscle.domain.MuscleRepository
import com.grippo.database.dao.MuscleDao
import com.grippo.database.mapper.muscles.toDomain
import com.grippo.network.Api
import com.grippo.network.mapper.muscles.toEntities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [MuscleRepository::class])
internal class MuscleRepositoryImpl(
    private val api: Api,
    private val muscleDao: MuscleDao,
) : MuscleRepository {

    override fun observeMuscles(): Flow<List<MuscleGroup>> {
        return muscleDao.get()
            .map { it.toDomain() }
    }

    override suspend fun getMuscles(): Result<Unit> {
        val response = api.getMuscles()

        response.onSuccess { r ->
            val groups = r.toEntities()
            val muscles = r.mapNotNull { it.muscles?.toEntities() }.flatten()
            muscleDao.insertOrUpdate(groups, muscles)
        }

        return response.map { }
    }
}