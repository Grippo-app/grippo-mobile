package com.grippo.data.features.excluded.equipments.domain

import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single(binds = [ExcludedEquipmentsFeature::class])
internal class ExcludedEquipmentsFeatureImpl(
    private val repository: ExcludedEquipmentsRepository
) : ExcludedEquipmentsFeature {

    override fun observeExcludedEquipments(): Flow<List<Equipment>> {
        return repository.observeExcludedEquipments()
    }

    override suspend fun getExcludedEquipments(): Result<Unit> {
        return repository.getExcludedEquipments()
    }

    override suspend fun setExcludedEquipments(ids: List<String>): Result<Unit> {
        return repository.setExcludedEquipments(ids)
    }
}