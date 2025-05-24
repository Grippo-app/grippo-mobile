package com.grippo.domain.mapper.equipment

import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.presentation.api.equipment.models.EquipmentGroupState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

public fun List<EquipmentGroup>.toState(): PersistentList<EquipmentGroupState> {
    return mapNotNull { it.toState() }.toPersistentList()
}

public fun EquipmentGroup.toState(): EquipmentGroupState? {
    return EquipmentGroupState(
        id = id,
        name = name,
        equipments = equipments.toState(),
    )
}