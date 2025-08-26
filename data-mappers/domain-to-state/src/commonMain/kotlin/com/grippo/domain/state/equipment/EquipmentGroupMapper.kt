package com.grippo.domain.state.equipment

import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.state.equipments.EquipmentGroupState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

public fun List<EquipmentGroup>.toState(): PersistentList<EquipmentGroupState> {
    return map { it.toState() }.toPersistentList()
}

public fun EquipmentGroup.toState(): EquipmentGroupState {
    return EquipmentGroupState(
        id = id,
        type = type.toState(),
        equipments = equipments.toState(),
    )
}