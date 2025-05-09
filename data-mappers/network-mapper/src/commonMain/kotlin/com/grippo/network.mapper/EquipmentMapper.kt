package com.grippo.network.mapper

import com.grippo.database.entity.EquipmentEntity
import com.grippo.network.dto.EquipmentDto

public fun List<EquipmentDto>.toEntities(): List<EquipmentEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun EquipmentDto.toEntityOrNull(): EquipmentEntity? {
    return EquipmentEntity(
        id = id ?: return null,
        equipmentGroupId = equipmentGroupId ?: return null,
        name = name ?: return null,
        type = type ?: return null,
        createdAt = createdAt ?: return null,
        updatedAt = updatedAt ?: return null,
    )
}