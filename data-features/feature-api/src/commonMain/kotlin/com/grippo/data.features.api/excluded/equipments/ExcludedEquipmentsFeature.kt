package com.grippo.data.features.api.excluded.equipments

import com.grippo.data.features.api.equipment.models.Equipment
import kotlinx.coroutines.flow.Flow

public interface ExcludedEquipmentsFeature {

    public fun observeExcludedEquipments(): Flow<List<Equipment>>

    public suspend fun getExcludedEquipments(): Result<Unit>

    public suspend fun setExcludedEquipments(ids: List<String>): Result<Unit>
}