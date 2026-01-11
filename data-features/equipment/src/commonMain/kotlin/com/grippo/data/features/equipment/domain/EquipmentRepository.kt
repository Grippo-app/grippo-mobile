package com.grippo.data.features.equipment.domain

import com.grippo.data.features.api.equipment.models.EquipmentGroup
import kotlinx.coroutines.flow.Flow

internal interface EquipmentRepository {

    fun observeEquipments(): Flow<List<EquipmentGroup>>

    suspend fun getEquipments(): Result<Unit>
}