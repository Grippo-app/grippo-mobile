package com.grippo.data.features.excluded.muscles.data

import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.excluded.muscles.domain.ExcludedMusclesRepository
import com.grippo.database.dao.UserActiveDao
import com.grippo.database.dao.UserDao
import com.grippo.database.entity.UserExcludedMuscleEntity
import com.grippo.entity.domain.muscles.toDomain
import com.grippo.network.Api
import com.grippo.network.mapper.muscles.toEntities
import com.grippo.network.user.IdsBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ExcludedMusclesRepository::class])
internal class ExcludedMusclesRepositoryImpl(
    private val api: Api,
    private val userDao: UserDao,
    private val userActiveDao: UserActiveDao,
) : ExcludedMusclesRepository {

    override fun observeExcludedMuscles(): Flow<List<Muscle>> {
        return userDao.getExcludedMuscles()
            .map { it.toDomain() }
    }

    override suspend fun getExcludedMuscles(): Result<Unit> {
        val response = api.getExcludedMuscles()

        response.onSuccess {
            val id = userActiveDao.get().firstOrNull() ?: return@onSuccess
            val userId = userDao.getById(id).firstOrNull()?.id ?: return@onSuccess
            val entities = it
                .mapNotNull { m -> m.id }
                .map { id -> UserExcludedMuscleEntity(userId, id) }

            userDao.insertOrReplaceExcludedMuscles(entities)
        }

        return response.map { }
    }

    override suspend fun setExcludedMuscles(ids: List<String>): Result<Unit> {
        val response = api.postExcludedMuscles(IdsBody(ids))

        response.onSuccess {
            val id = userActiveDao.get().firstOrNull() ?: return@onSuccess
            val userId = userDao.getById(id).firstOrNull()?.id ?: return@onSuccess
            val entities = api.getExcludedMuscles()
                .getOrNull()
                ?.toEntities()
                ?.map { UserExcludedMuscleEntity(userId = userId, muscleId = it.id) }
                ?: return@onSuccess

            userDao.insertOrReplaceExcludedMuscles(entities)
        }

        return response.map { }
    }
}