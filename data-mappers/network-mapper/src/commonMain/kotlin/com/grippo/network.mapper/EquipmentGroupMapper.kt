package com.grippo.network.mapper

import com.grippo.database.entity.EquipmentEntity
import com.grippo.database.entity.EquipmentGroupEntity
import com.grippo.network.dto.EquipmentGroupDto

public fun EquipmentGroupDto.toEquipments(): List<EquipmentEntity> {
    return equipments.orEmpty().mapNotNull { it.toEntityOrNull() }
}

public fun EquipmentGroupDto.toEntityOrNull(): EquipmentGroupEntity? {
    return EquipmentGroupEntity(
        id = id ?: return null,
        name = name ?: return null,
        type = type ?: return null,
        createdAt = createdAt ?: return null,
        updatedAt = updatedAt ?: return null,
    )
}