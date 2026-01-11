package com.grippo.entity.domain.equipment

import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.data.features.api.equipment.models.EquipmentGroupEnum
import com.grippo.services.database.models.EquipmentGroupWithEquipments
import com.grippo.toolkit.logger.AppLogger

public fun List<EquipmentGroupWithEquipments>.toDomain(): List<EquipmentGroup> {
    return mapNotNull { it.toDomain() }
}

public fun EquipmentGroupWithEquipments.toDomain(): EquipmentGroup? {
    val mappedType = AppLogger.Mapping.log(EquipmentGroupEnum.of(group.type)) {
        "EquipmentGroupEnum ${group.id} has unrecognized type: ${group.type}"
    } ?: return null

    return EquipmentGroup(
        id = group.id,
        name = group.name,
        type = mappedType,
        equipments = equipments.toDomain(),
    )
}
