package com.grippo.data.features.excluded.muscles.data

import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.excluded.muscles.domain.ExcludedMusclesRepository
import com.grippo.database.dao.UserDao
import com.grippo.database.entity.UserExcludedMuscleEntity
import com.grippo.database.mapper.toDomain
import com.grippo.network.Api
import com.grippo.network.mapper.toEntities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

internal class ExcludedMusclesRepositoryImpl(
    private val api: Api,
    private val userDao: UserDao,
) : ExcludedMusclesRepository {

    override fun observeExcludedMuscles(): Flow<List<Muscle>> {
        return userDao.get()
            .mapNotNull { it?.id }
            .flatMapLatest { userDao.getExcludedMuscles(it) }
            .map { it.toDomain() }
    }

    override suspend fun getExcludedMuscles(): Result<Unit> {
        val response = api.getExcludedMuscles()

        response.onSuccess {
            val userId = userDao.get().firstOrNull()?.id ?: return@onSuccess
            val entities = it
                .mapNotNull { it.id }
                .map { id -> UserExcludedMuscleEntity(userId, id) }

            userDao.replaceExcludedMuscles(userId, entities)
        }

        return response.map { }
    }

    override suspend fun setExcludedMuscle(id: String): Result<Unit> {
        val response = api.setExcludedMuscle(id)

        response.onSuccess {
            val muscles = api.getExcludedMuscles().getOrNull()?.toEntities() ?: return@onSuccess
            val userId = userDao.get().firstOrNull()?.id ?: return@onSuccess
            val entities = muscles.map { UserExcludedMuscleEntity(userId, it.id) }
            userDao.replaceExcludedMuscles(userId, entities)
        }

        return response.map { }
    }

    override suspend fun deleteExcludedMuscle(id: String): Result<Unit> {
        val response = api.deleteExcludedMuscle(id)

        response.onSuccess {
            val muscles = api.getExcludedMuscles().getOrNull()?.toEntities() ?: return@onSuccess
            val userId = userDao.get().firstOrNull()?.id ?: return@onSuccess
            val entities = muscles.map { UserExcludedMuscleEntity(userId, it.id) }
            userDao.replaceExcludedMuscles(userId, entities)
        }

        return response.map { }
    }
}