package com.grippo.data.features.excluded.muscles.data

import com.grippo.core.error.provider.AppError
import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.excluded.muscles.domain.ExcludedMusclesRepository
import com.grippo.entity.domain.muscles.toDomain
import com.grippo.services.backend.GrippoApi
import com.grippo.services.backend.dto.user.IdsBody
import com.grippo.services.database.dao.UserActiveDao
import com.grippo.services.database.dao.UserDao
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

    override suspend fun getExcludedMuscles(): Unit {
        val profileId = getActiveProfileId()
            ?: throw AppError.Expected(
                message = "An active profile is required to load excluded muscles",
                description = null,
            )
        val response = api.getExcludedMuscles()
        val ids = response.mapNotNull { it.id }
        if (ids.size != response.size) {
            throw AppError.Network.IncompleteResponse()
        }
        userDao.insertOrReplaceExcludedMuscles(profileId, ids)
    }

    override suspend fun setExcludedMuscles(ids: List<String>): Unit {
        val profileId = getActiveProfileId()
            ?: throw AppError.Expected(
                message = "An active profile is required to save excluded muscles",
                description = null,
            )
        api.postExcludedMuscles(IdsBody(ids))

        val muscles = api.getExcludedMuscles()
        val savedIds = muscles.mapNotNull { it.id }
        if (savedIds.size != muscles.size) {
            throw AppError.Network.IncompleteResponse()
        }
        userDao.insertOrReplaceExcludedMuscles(profileId, savedIds)
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
