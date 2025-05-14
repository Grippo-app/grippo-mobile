package com.grippo.data.features.weight.history.data

import com.grippo.data.features.api.weight.history.models.WeightHistory
import com.grippo.data.features.weight.history.domain.WeightHistoryRepository
import com.grippo.database.dao.WeightHistoryDao
import com.grippo.database.mapper.toDomain
import com.grippo.network.Api
import com.grippo.network.mapper.toEntities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class WeightHistoryRepositoryImpl(
    private val api: Api,
    private val weightHistoryDao: WeightHistoryDao,
) : WeightHistoryRepository {

    override fun observeWeightHistory(): Flow<List<WeightHistory>> {
        return weightHistoryDao.get()
            .map { it.toDomain() }
    }

    override fun observeLastWeight(): Flow<WeightHistory?> {
        return weightHistoryDao.getLast()
            .map { it?.toDomain() }
    }

    override suspend fun getWeightHistory(): Result<Unit> {
        val response = api.getWeightHistory()

        response.onSuccess {
            weightHistoryDao.insertOrUpdate(it.toEntities())
        }

        return response.map { }
    }

    override suspend fun updateWeight(value: Float): Result<Unit> {
        val response = api.updateWeightHistory(value)

        response.onSuccess {
            val list = api.getWeightHistory().getOrNull() ?: return@onSuccess
            weightHistoryDao.insertOrUpdate(list.toEntities())
        }

        return response.map { }
    }

    override suspend fun removeWeight(id: String): Result<Unit> {
        val response = api.removeWeight(id)

        response.onSuccess {
            val list = api.getWeightHistory().getOrNull() ?: return@onSuccess
            weightHistoryDao.insertOrUpdate(list.toEntities())
        }

        return response.map { }
    }
}