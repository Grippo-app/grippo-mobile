package com.grippo.database.mapper.equipment

import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.equipment.models.EquipmentEnum
import com.grippo.database.entity.EquipmentEntity

public fun List<EquipmentEntity>.toDomain(): List<Equipment> {
    return map { it.toDomain() }
}

public fun EquipmentEntity.toDomain(): Equipment {
    return Equipment(
        id = id,
        name = name,
        type = EquipmentEnum.of(type),
    )
}