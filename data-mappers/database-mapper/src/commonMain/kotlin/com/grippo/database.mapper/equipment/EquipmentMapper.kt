package com.grippo.database.mapper.equipment

import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.equipment.models.EquipmentEnum
import com.grippo.database.entity.EquipmentEntity
import com.grippo.logger.AppLogger

public fun List<EquipmentEntity>.toDomain(): List<Equipment> {
    return mapNotNull { it.toDomain() }
}

public fun EquipmentEntity.toDomain(): Equipment? {
    val mappedType = AppLogger.Mapping.log(EquipmentEnum.of(type)) {
        "EquipmentEntity $id has unrecognized type: $type"
    } ?: return null

    return Equipment(
        id = id,
        name = name,
        type = mappedType
    )
}
