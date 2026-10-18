package com.grippo.data.features.weight.history.data

import com.grippo.core.error.provider.AppError
import com.grippo.data.features.api.weight.history.models.WeightHistory
import com.grippo.data.features.weight.history.domain.WeightHistoryRepository
import com.grippo.dto.entity.user.toEntities
import com.grippo.entity.domain.user.toDomain
import com.grippo.services.backend.GrippoApi
import com.grippo.services.database.dao.UserActiveDao
import com.grippo.services.database.dao.UserDao
import com.grippo.services.database.dao.WeightHistoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [WeightHistoryRepository::class])
internal class WeightHistoryRepositoryImpl(
    private val api: GrippoApi,
    private val weightHistoryDao: WeightHistoryDao,
    private val userActiveDao: UserActiveDao,
    private val userDao: UserDao,
) : WeightHistoryRepository {

    override fun observeWeightHistory(): Flow<List<WeightHistory>> {
        return weightHistoryDao.get()
            .map { it.toDomain() }
    }

    override fun observeLastWeight(): Flow<WeightHistory?> {
        return weightHistoryDao.getLast()
            .map { it?.toDomain() }
    }

    override suspend fun getWeightHistory(): Unit {
        val userId = userActiveDao.get().firstOrNull()
            ?: throw AppError.Expected(
                message = "An active user is required to load weight history",
                description = null,
            )
        val response = api.getWeightHistory()

        val entities = response.toEntities(userId)
        if (entities.size != response.size || entities.map { it.id }.toSet().size != entities.size) {
            throw AppError.Network.IncompleteResponse()
        }
        entities.forEach { it.toDomain() }
        weightHistoryDao.replaceByUserId(userId = userId, values = entities)
    }

    override suspend fun updateWeight(value: Float): Unit {
        val userId = requireCachedActiveUserId()
        api.updateWeightHistory(value)

        val response = api.getWeightHistory()
        val entities = response.toEntities(userId)
        if (entities.size != response.size || entities.map { it.id }.toSet().size != entities.size) {
            throw AppError.Network.IncompleteResponse()
        }
        entities.forEach { it.toDomain() }
        weightHistoryDao.replaceByUserId(userId = userId, values = entities)
    }

    override suspend fun deleteWeight(id: String): Unit {
        val userId = requireCachedActiveUserId()
        api.deleteWeight(id)

        val response = api.getWeightHistory()
        val entities = response.toEntities(userId)
        if (entities.size != response.size || entities.map { it.id }.toSet().size != entities.size) {
            throw AppError.Network.IncompleteResponse()
        }
        entities.forEach { it.toDomain() }
        weightHistoryDao.replaceByUserId(userId = userId, values = entities)
    }

    private suspend fun requireCachedActiveUserId(): String {
        return userActiveDao.get().firstOrNull()
            ?.let { userDao.getById(it).firstOrNull()?.id }
            ?: throw AppError.Expected(
                message = "An active user profile is required to change weight history",
                description = null,
            )
    }
}
