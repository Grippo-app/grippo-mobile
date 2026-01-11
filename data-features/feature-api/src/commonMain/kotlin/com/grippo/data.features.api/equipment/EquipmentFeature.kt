package com.grippo.data.features.api.equipment

import com.grippo.data.features.api.equipment.models.EquipmentGroup
import kotlinx.coroutines.flow.Flow

public interface EquipmentFeature {

    public fun observeEquipments(): Flow<List<EquipmentGroup>>

    public suspend fun getEquipments(): Result<Unit>
}