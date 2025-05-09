package com.grippo.data.features.equipment.data

import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.data.features.equipment.domain.EquipmentRepository
import com.grippo.database.dao.EquipmentDao
import com.grippo.database.mapper.toDomain
import com.grippo.network.Api
import com.grippo.network.mapper.toEntities
import com.grippo.network.mapper.toEntityOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class EquipmentRepositoryImpl(
    private val api: Api,
    private val equipmentDao: EquipmentDao,
) : EquipmentRepository {

    override fun observeEquipments(): Flow<List<EquipmentGroup>> {
        return equipmentDao.getGroups()
            .map { it.toDomain() }
    }

    override suspend fun getPublicEquipments(): Result<Unit> {
        val response = api.getPublicEquipments()

        response.onSuccess { r ->
            val groups = r.mapNotNull { it.toEntityOrNull() }
            val equipments = r.mapNotNull { it.equipments?.toEntities() }.flatten()
            equipmentDao.insertGroupsWithEquipments(groups, equipments)
        }

        return response.map { }
    }
}