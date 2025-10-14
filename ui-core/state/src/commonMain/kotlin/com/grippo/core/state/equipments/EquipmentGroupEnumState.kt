package com.grippo.core.state.equipments

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.equipment_group_benches_and_racks
import com.grippo.design.resources.provider.equipment_group_body_weight
import com.grippo.design.resources.provider.equipment_group_cable_machines
import com.grippo.design.resources.provider.equipment_group_free_weight
import com.grippo.design.resources.provider.equipment_group_machines

@Immutable
public enum class EquipmentGroupEnumState {
    FREE_WEIGHT,
    MACHINES,
    CABLE_MACHINES,
    BODY_WEIGHT,
    BENCHES_AND_RACKS;

    public fun title(): UiText {
        val r = when (this) {
            FREE_WEIGHT -> Res.string.equipment_group_free_weight
            MACHINES -> Res.string.equipment_group_machines
            CABLE_MACHINES -> Res.string.equipment_group_cable_machines
            BODY_WEIGHT -> Res.string.equipment_group_body_weight
            BENCHES_AND_RACKS -> Res.string.equipment_group_benches_and_racks
        }
        return UiText.Res(r)
    }
}