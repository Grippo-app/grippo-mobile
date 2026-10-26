package com.grippo.data.features.equipment.data

import com.grippo.core.error.provider.AppError
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.data.features.equipment.domain.EquipmentRepository
import com.grippo.dto.entity.equipment.toEntities
import com.grippo.entity.domain.equipment.toDomain
import com.grippo.services.backend.GrippoApi
import com.grippo.services.database.dao.EquipmentDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [EquipmentRepository::class])
internal class EquipmentRepositoryImpl(
    private val api: GrippoApi,
    private val equipmentDao: EquipmentDao,
) : EquipmentRepository {

    override fun observeEquipments(): Flow<List<EquipmentGroup>> {
        return equipmentDao.get()
            .map { it.toDomain() }
    }

    override suspend fun getEquipments(): Unit {
        val response = api.getEquipments()

        val groups = response.toEntities()
        val rawEquipments = response.flatMap {
            it.equipments ?: throw AppError.Network.IncompleteResponse()
        }
        val equipments = rawEquipments.toEntities()
        if (groups.size != response.size || equipments.size != rawEquipments.size) {
            throw AppError.Network.IncompleteResponse()
        }
        if (groups.map { it.id }.toSet().size != groups.size ||
            equipments.map { it.id }.toSet().size != equipments.size ||
            response.any { group ->
                group.equipments.orEmpty().any { it.equipmentGroupId != group.id }
            }
        ) {
            throw AppError.Network.IncompleteResponse()
        }
        equipmentDao.insertOrUpdate(groups, equipments)
    }
}
