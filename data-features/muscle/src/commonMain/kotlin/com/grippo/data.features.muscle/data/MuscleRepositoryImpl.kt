package com.grippo.data.features.muscle.data

import com.grippo.core.error.provider.AppError
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.muscle.domain.MuscleRepository
import com.grippo.dto.entity.muscles.toEntities
import com.grippo.entity.domain.muscles.toDomain
import com.grippo.services.backend.GrippoApi
import com.grippo.services.database.dao.MuscleDao
import com.grippo.services.database.models.MuscleGroupWithMuscles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [MuscleRepository::class])
internal class MuscleRepositoryImpl(
    private val api: GrippoApi,
    private val muscleDao: MuscleDao,
) : MuscleRepository {

    override fun observeMuscles(): Flow<List<MuscleGroup>> {
        return muscleDao.get()
            .map { it.toDomain() }
    }

    override suspend fun getMuscles(): Unit {
        val response = api.getMuscles()

        val groups = response.toEntities()
        val rawMuscles = response.flatMap {
            it.muscles ?: throw AppError.Network.IncompleteResponse()
        }
        val muscles = rawMuscles.toEntities()
        if (groups.size != response.size || muscles.size != rawMuscles.size) {
            throw AppError.Network.IncompleteResponse()
        }
        if (groups.map { it.id }.toSet().size != groups.size ||
            muscles.map { it.id }.toSet().size != muscles.size ||
            response.any { group ->
                group.muscles.orEmpty().any { it.muscleGroupId != group.id }
            }
        ) {
            throw AppError.Network.IncompleteResponse()
        }
        val musclesByGroup = muscles.groupBy { it.muscleGroupId }
        for (group in groups) {
            val groupMuscles = musclesByGroup[group.id].orEmpty()
            val pack = MuscleGroupWithMuscles(group = group, muscles = groupMuscles)
            val domain = pack.toDomain() ?: throw AppError.Network.IncompleteResponse()
            if (domain.muscles.size != groupMuscles.size) {
                throw AppError.Network.IncompleteResponse()
            }
        }
        muscleDao.insertOrUpdate(groups, muscles)
    }
}
