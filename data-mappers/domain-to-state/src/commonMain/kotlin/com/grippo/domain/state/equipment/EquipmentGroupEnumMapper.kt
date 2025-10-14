package com.grippo.domain.state.equipment

import com.grippo.core.state.equipments.EquipmentGroupEnumState
import com.grippo.data.features.api.equipment.models.EquipmentGroupEnum

public fun EquipmentGroupEnum.toState(): EquipmentGroupEnumState {
    return when (this) {
        EquipmentGroupEnum.FREE_WEIGHT -> EquipmentGroupEnumState.FREE_WEIGHT
        EquipmentGroupEnum.MACHINES -> EquipmentGroupEnumState.MACHINES
        EquipmentGroupEnum.CABLE_MACHINES -> EquipmentGroupEnumState.CABLE_MACHINES
        EquipmentGroupEnum.BODY_WEIGHT -> EquipmentGroupEnumState.BODY_WEIGHT
        EquipmentGroupEnum.BENCHES_AND_RACKS -> EquipmentGroupEnumState.BENCHES_AND_RACKS
    }
}
