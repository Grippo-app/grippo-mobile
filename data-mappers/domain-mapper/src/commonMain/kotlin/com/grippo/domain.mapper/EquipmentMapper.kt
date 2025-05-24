package com.grippo.domain.mapper

import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.presentation.api.equipment.models.EquipmentState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<Equipment>.toState(): ImmutableList<EquipmentState> {
    return mapNotNull { it.toState() }.toPersistentList()
}

public fun Equipment.toState(): EquipmentState? {
    return EquipmentState(
        id = id,
        name = name,
        type = type.toState() ?: return null,
    )
}