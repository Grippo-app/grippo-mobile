package com.grippo.database.domain.equipment

import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.database.models.EquipmentGroupWithEquipments

public fun List<EquipmentGroupWithEquipments>.toDomain(): List<EquipmentGroup> {
    return mapNotNull { it.toDomain() }
}

public fun EquipmentGroupWithEquipments.toDomain(): EquipmentGroup {
    return EquipmentGroup(
        id = group.id,
        name = group.name,
        equipments = equipments.toDomain(),
    )
}
