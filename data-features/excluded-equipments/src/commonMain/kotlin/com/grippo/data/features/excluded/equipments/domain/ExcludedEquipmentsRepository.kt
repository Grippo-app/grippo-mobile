package com.grippo.data.features.excluded.equipments.domain

import com.grippo.data.features.api.equipment.models.Equipment
import kotlinx.coroutines.flow.Flow

internal interface ExcludedEquipmentsRepository {
    fun observeExcludedEquipments(): Flow<List<Equipment>>

    suspend fun getExcludedEquipments(): Result<Unit>
    suspend fun setExcludedEquipments(ids: List<String>): Result<Unit>
}