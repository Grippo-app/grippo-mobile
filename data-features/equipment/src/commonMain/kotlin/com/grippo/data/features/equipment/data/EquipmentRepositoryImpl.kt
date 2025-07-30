package com.grippo.data.features.equipment.data

import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.data.features.equipment.domain.EquipmentRepository
import com.grippo.database.dao.EquipmentDao
import com.grippo.entity.domain.equipment.toDomain
import com.grippo.network.Api
import com.grippo.network.mapper.equipment.toEntities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [EquipmentRepository::class])
internal class EquipmentRepositoryImpl(
    private val api: Api,
    private val equipmentDao: EquipmentDao,
) : EquipmentRepository {

    override fun observeEquipments(): Flow<List<EquipmentGroup>> {
        return equipmentDao.get()
            .map { it.toDomain() }
    }

    override suspend fun getEquipments(): Result<Unit> {
        val response = api.getEquipments()

        response.onSuccess { r ->
            val groups = r.toEntities()
            val equipments = r.mapNotNull { it.equipments?.toEntities() }.flatten()
            equipmentDao.insertOrUpdate(groups, equipments)
        }

        return response.map { }
    }
}