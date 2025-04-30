package com.grippo.data.features.api.equipment

import com.grippo.data.features.api.equipment.models.EquipmentGroup
import kotlinx.coroutines.flow.Flow

public interface EquipmentFeature {
    public fun observeEquipments(): Flow<List<EquipmentGroup>>
    public suspend fun getUserEquipments(): Result<Unit>
    public suspend fun getPublicEquipments(): Result<Unit>
}