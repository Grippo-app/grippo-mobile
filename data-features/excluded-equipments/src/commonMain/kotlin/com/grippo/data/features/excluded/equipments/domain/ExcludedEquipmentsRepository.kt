package com.grippo.data.features.excluded.equipments.domain

import com.grippo.data.features.api.equipment.models.Equipment
import kotlinx.coroutines.flow.Flow

internal interface ExcludedEquipmentsRepository {
    fun observeExcludedEquipments(): Flow<List<Equipment>>

    suspend fun getExcludedEquipments(): Result<Unit>
    suspend fun setExcludedEquipment(id: String): Result<Unit>
    suspend fun deleteExcludedEquipment(id: String): Result<Unit>
}