package com.grippo.data.features.exercise.examples.domain

import com.grippo.data.features.api.equipment.models.EquipmentGroup
import kotlinx.coroutines.flow.Flow

internal interface EquipmentRepository {
    fun observeEquipments(): Flow<List<EquipmentGroup>>

    suspend fun getPublicEquipments(): Result<Unit>
}