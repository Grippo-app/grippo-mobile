package com.grippo.data.features.excluded.muscles.data

import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.excluded.muscles.domain.ExcludedMusclesRepository
import com.grippo.database.dao.UserActiveDao
import com.grippo.database.dao.UserDao
import com.grippo.database.domain.muscles.toDomain
import com.grippo.network.Api
import com.grippo.network.dto.user.IdsBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ExcludedMusclesRepository::class])
internal class ExcludedMusclesRepositoryImpl(
    private val api: Api,
    private val userDao: UserDao,
    private val userActiveDao: UserActiveDao,
) : ExcludedMusclesRepository {

    override fun observeExcludedMuscles(): Flow<List<Muscle>> {
        return userActiveDao.get()
            .flatMapLatest { userId ->
                if (userId.isNullOrEmpty()) flowOf(emptyList())
                else userDao.getExcludedMuscles(userId).map { it.toDomain() }
            }
    }

    override suspend fun getExcludedMuscles(): Result<Unit> {
        val response = api.getExcludedMuscles()

        response.onSuccess {
            val userId = userActiveDao.get()
                .firstOrNull()
                ?: return@onSuccess

            val ids = it.mapNotNull { m -> m.id }

            userDao.insertOrReplaceExcludedMuscles(userId, ids)
        }

        return response.map { }
    }

    override suspend fun setExcludedMuscles(ids: List<String>): Result<Unit> {
        val response = api.postExcludedMuscles(IdsBody(ids))

        response.onSuccess {
            val userId = userActiveDao.get()
                .firstOrNull()
                ?: return@onSuccess

            val ids = api.getExcludedMuscles()
                .getOrNull()
                ?.mapNotNull { it.id }
                ?: return@onSuccess

            userDao.insertOrReplaceExcludedMuscles(userId, ids)
        }

        return response.map { }
    }
}
