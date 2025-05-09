package com.grippo.data.features.equipment.domain

import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import kotlinx.coroutines.flow.Flow

internal class EquipmentFeatureImpl(
    private val repository: EquipmentRepository
) : EquipmentFeature {

    override fun observeEquipments(): Flow<List<EquipmentGroup>> {
        return repository.observeEquipments()
    }

    override suspend fun getPublicEquipments(): Result<Unit> {
        return repository.getPublicEquipments()
    }
}