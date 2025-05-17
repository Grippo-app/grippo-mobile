package com.grippo.data.features.excluded.equipments.data

import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.excluded.equipments.domain.ExcludedEquipmentsRepository
import com.grippo.database.dao.UserDao
import com.grippo.database.entity.UserExcludedEquipmentEntity
import com.grippo.database.mapper.toDomain
import com.grippo.network.Api
import com.grippo.network.mapper.toEntities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

internal class ExcludedEquipmentsRepositoryImpl(
    private val api: Api,
    private val userDao: UserDao,
) : ExcludedEquipmentsRepository {

    override fun observeExcludedEquipments(): Flow<List<Equipment>> {
        return userDao.getExcludedEquipments()
            .map { it.toDomain() }
    }

    override suspend fun getExcludedEquipments(): Result<Unit> {
        val response = api.getExcludedEquipments()

        response.onSuccess {
            val userId = userDao.get().firstOrNull()?.id ?: return@onSuccess
            val entities = it
                .mapNotNull { it.id }
                .map { id -> UserExcludedEquipmentEntity(userId, id) }

            userDao.replaceExcludedEquipments(entities)
        }

        return response.map { }
    }

    override suspend fun setExcludedEquipment(id: String): Result<Unit> {
        val response = api.setExcludedEquipment(id)

        response.onSuccess {
            val equipments =
                api.getExcludedEquipments().getOrNull()?.toEntities() ?: return@onSuccess
            val userId = userDao.get().firstOrNull()?.id ?: return@onSuccess
            val entities = equipments.map { UserExcludedEquipmentEntity(userId, it.id) }
            userDao.replaceExcludedEquipments(entities)
        }

        return response.map { }
    }

    override suspend fun deleteExcludedEquipment(id: String): Result<Unit> {
        val response = api.deleteExcludedEquipment(id)

        response.onSuccess {
            val equipments =
                api.getExcludedEquipments().getOrNull()?.toEntities() ?: return@onSuccess
            val userId = userDao.get().firstOrNull()?.id ?: return@onSuccess
            val entities = equipments.map { UserExcludedEquipmentEntity(userId, it.id) }
            userDao.replaceExcludedEquipments(entities)
        }

        return response.map { }
    }
}