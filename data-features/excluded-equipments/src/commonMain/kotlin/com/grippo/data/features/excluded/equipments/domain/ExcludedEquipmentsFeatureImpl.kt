package com.grippo.data.features.excluded.equipments.domain

import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import kotlinx.coroutines.flow.Flow

internal class ExcludedEquipmentsFeatureImpl(
    private val repository: ExcludedEquipmentsRepository
) : ExcludedEquipmentsFeature {

    override fun observeExcludedEquipments(): Flow<List<Equipment>> {
        return repository.observeExcludedEquipments()
    }

    override suspend fun getExcludedEquipments(): Result<Unit> {
        return repository.getExcludedEquipments()
    }

    override suspend fun setExcludedEquipment(id: String): Result<Unit> {
        return repository.setExcludedEquipment(id)
    }

    override suspend fun deleteExcludedEquipment(id: String): Result<Unit> {
        return repository.deleteExcludedEquipment(id)
    }
}