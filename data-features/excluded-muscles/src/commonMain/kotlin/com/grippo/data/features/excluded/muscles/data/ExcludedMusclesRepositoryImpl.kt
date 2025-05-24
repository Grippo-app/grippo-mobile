package com.grippo.data.features.excluded.muscles.data

import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.excluded.muscles.domain.ExcludedMusclesRepository
import com.grippo.database.dao.UserDao
import com.grippo.database.entity.UserExcludedMuscleEntity
import com.grippo.database.mapper.muscles.toDomain
import com.grippo.network.Api
import com.grippo.network.mapper.muscles.toEntities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

internal class ExcludedMusclesRepositoryImpl(
    private val api: Api,
    private val userDao: UserDao,
) : ExcludedMusclesRepository {

    override fun observeExcludedMuscles(): Flow<List<Muscle>> {
        return userDao.getExcludedMuscles()
            .map { it.toDomain() }
    }

    override suspend fun getExcludedMuscles(): Result<Unit> {
        val response = api.getExcludedMuscles()

        response.onSuccess {
            val userId = userDao.get().firstOrNull()?.id ?: return@onSuccess
            val entities = it
                .mapNotNull { it.id }
                .map { id -> UserExcludedMuscleEntity(userId, id) }

            userDao.insertOrReplaceExcludedMuscles(entities)
        }

        return response.map { }
    }

    override suspend fun setExcludedMuscle(id: String): Result<Unit> {
        val response = api.setExcludedMuscle(id)

        response.onSuccess {
            val userId = userDao.get().firstOrNull()?.id ?: return@onSuccess
            val entities = api.getExcludedMuscles()
                .getOrNull()
                ?.toEntities()
                ?.map { UserExcludedMuscleEntity(userId, it.id) } ?: return@onSuccess

            userDao.insertOrReplaceExcludedMuscles(entities)
        }

        return response.map { }
    }

    override suspend fun deleteExcludedMuscle(id: String): Result<Unit> {
        val response = api.deleteExcludedMuscle(id)

        response.onSuccess {
            val userId = userDao.get().firstOrNull()?.id ?: return@onSuccess
            val entities = api.getExcludedMuscles()
                .getOrNull()
                ?.toEntities()
                ?.map { UserExcludedMuscleEntity(userId, it.id) } ?: return@onSuccess

            userDao.insertOrReplaceExcludedMuscles(entities)
        }

        return response.map { }
    }
}