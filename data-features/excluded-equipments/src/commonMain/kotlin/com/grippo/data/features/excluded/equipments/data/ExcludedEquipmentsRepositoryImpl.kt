package com.grippo.data.features.excluded.equipments.data

import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.excluded.equipments.domain.ExcludedEquipmentsRepository
import com.grippo.database.dao.UserActiveDao
import com.grippo.database.dao.UserDao
import com.grippo.database.domain.equipment.toDomain
import com.grippo.database.entity.UserExcludedEquipmentEntity
import com.grippo.network.Api
import com.grippo.network.database.equipment.toEntities
import com.grippo.network.user.IdsBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ExcludedEquipmentsRepository::class])
internal class ExcludedEquipmentsRepositoryImpl(
    private val api: Api,
    private val userDao: UserDao,
    private val userActiveDao: UserActiveDao
) : ExcludedEquipmentsRepository {

    override fun observeExcludedEquipments(): Flow<List<Equipment>> {
        return userDao.getExcludedEquipments()
            .map { it.toDomain() }
    }

    override suspend fun getExcludedEquipments(): Result<Unit> {
        val response = api.getExcludedEquipments()

        response.onSuccess {
            val userId = userActiveDao.get().firstOrNull() ?: return@onSuccess
            val entities = it
                .mapNotNull { m -> m.id }
                .map { id -> UserExcludedEquipmentEntity(userId, id) }

            userDao.insertOrReplaceExcludedEquipments(entities)
        }

        return response.map { }
    }

    override suspend fun setExcludedEquipments(ids: List<String>): Result<Unit> {
        val response = api.postExcludedEquipments(IdsBody(ids))

        response.onSuccess {
            val userId = userActiveDao.get().firstOrNull() ?: return@onSuccess
            val entities = api.getExcludedEquipments()
                .getOrNull()
                ?.toEntities()
                ?.map { UserExcludedEquipmentEntity(userId = userId, equipmentId = it.id) }
                ?: return@onSuccess

            userDao.insertOrReplaceExcludedEquipments(entities)
        }

        return response.map { }
    }
}