package com.grippo.data.features.excluded.muscles.data

import com.grippo.backend.GrippoApi
import com.grippo.backend.dto.user.IdsBody
import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.excluded.muscles.domain.ExcludedMusclesRepository
import com.grippo.database.dao.UserActiveDao
import com.grippo.database.dao.UserDao
import com.grippo.entity.domain.muscles.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ExcludedMusclesRepository::class])
internal class ExcludedMusclesRepositoryImpl(
    private val api: GrippoApi,
    private val userDao: UserDao,
    private val userActiveDao: UserActiveDao,
) : ExcludedMusclesRepository {

    override fun observeExcludedMuscles(): Flow<List<Muscle>> {
        return observeActiveProfileId()
            .flatMapLatest { profileId ->
                if (profileId.isNullOrEmpty()) flowOf(emptyList())
                else userDao.getExcludedMuscles(profileId).map { it.toDomain() }
            }
    }

    override suspend fun getExcludedMuscles(): Result<Unit> {
        val response = api.getExcludedMuscles()

        response.onSuccess {
            val profileId = getActiveProfileId() ?: return@onSuccess

            val ids = it.mapNotNull { m -> m.id }

            userDao.insertOrReplaceExcludedMuscles(profileId, ids)
        }

        return response.map { }
    }

    override suspend fun setExcludedMuscles(ids: List<String>): Result<Unit> {
        val response = api.postExcludedMuscles(IdsBody(ids))

        response.onSuccess {
            val profileId = getActiveProfileId() ?: return@onSuccess

            val ids = api.getExcludedMuscles()
                .getOrNull()
                ?.mapNotNull { it.id }
                ?: return@onSuccess

            userDao.insertOrReplaceExcludedMuscles(profileId, ids)
        }

        return response.map { }
    }

    private fun observeActiveProfileId(): Flow<String?> {
        return userActiveDao.get()
            .flatMapLatest { userId ->
                if (userId.isNullOrEmpty()) flowOf(null)
                else userDao.getById(userId).map { it?.profileId }
            }
    }

    private suspend fun getActiveProfileId(): String? {
        val userId = userActiveDao.get().firstOrNull() ?: return null
        return userDao.getById(userId).firstOrNull()?.profileId
    }
}
