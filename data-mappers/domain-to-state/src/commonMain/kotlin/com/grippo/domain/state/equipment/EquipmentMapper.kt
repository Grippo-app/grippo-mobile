package com.grippo.domain.state.equipment

import com.grippo.core.state.equipments.EquipmentState
import com.grippo.data.features.api.equipment.models.Equipment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<Equipment>.toState(): ImmutableList<EquipmentState> {
    return map { it.toState() }.toPersistentList()
}

public fun Equipment.toState(): EquipmentState {
    return EquipmentState(
        id = id,
        type = type.toState()
    )
}