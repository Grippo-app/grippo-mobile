package com.grippo.domain.state.equipment

import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.data.features.api.equipment.models.EquipmentEnum

public fun EquipmentEnum.toState(): EquipmentEnumState {
    return when (this) {
        EquipmentEnum.DUMBBELLS -> EquipmentEnumState.DUMBBELLS
        EquipmentEnum.BARBELL -> EquipmentEnumState.BARBELL
        EquipmentEnum.VBar -> EquipmentEnumState.V_BAR
        EquipmentEnum.WideGripHandle -> EquipmentEnumState.WIDE_GRIP_HANDLE
        EquipmentEnum.CloseGripHandle -> EquipmentEnumState.CLOSE_GRIP_HANDLE
        EquipmentEnum.EZ_BAR -> EquipmentEnumState.EZ_BAR
        EquipmentEnum.TRAP_BAR -> EquipmentEnumState.TRAP_BAR
        EquipmentEnum.ROPE -> EquipmentEnumState.ROPE
        EquipmentEnum.STRAIGHT_BAR -> EquipmentEnumState.STRAIGHT_BAR
        EquipmentEnum.CORD_HANDLES -> EquipmentEnumState.CORD_HANDLES

        EquipmentEnum.AB_MACHINES -> EquipmentEnumState.AB_MACHINES
        EquipmentEnum.BUTTERFLY -> EquipmentEnumState.BUTTERFLY
        EquipmentEnum.BUTTERFLY_REVERSE -> EquipmentEnumState.BUTTERFLY_REVERSE
        EquipmentEnum.LEG_EXTENSION_MACHINES -> EquipmentEnumState.LEG_EXTENSION_MACHINES
        EquipmentEnum.LEG_CURL_MACHINES -> EquipmentEnumState.LEG_CURL_MACHINES
        EquipmentEnum.CHEST_PRESS_MACHINES -> EquipmentEnumState.CHEST_PRESS_MACHINES
        EquipmentEnum.BICEPS_MACHINES -> EquipmentEnumState.BICEPS_MACHINES
        EquipmentEnum.SMITH_MACHINES -> EquipmentEnumState.SMITH_MACHINES
        EquipmentEnum.HACK_SQUAT_MACHINES -> EquipmentEnumState.HACK_SQUAT_MACHINES
        EquipmentEnum.LEG_PRESS_MACHINE -> EquipmentEnumState.LEG_PRESS_MACHINE
        EquipmentEnum.DEADLIFT_MACHINES -> EquipmentEnumState.DEADLIFT_MACHINES
        EquipmentEnum.SHOULDER_PRESS_MACHINES -> EquipmentEnumState.SHOULDER_PRESS_MACHINES
        EquipmentEnum.LATERAL_RAISE_MACHINES -> EquipmentEnumState.LATERAL_RAISE_MACHINES
        EquipmentEnum.TRICEPS_MACHINES -> EquipmentEnumState.TRICEPS_MACHINES
        EquipmentEnum.CALF_RAISE_MACHINES -> EquipmentEnumState.CALF_RAISE_MACHINES
        EquipmentEnum.GLUTE_MACHINES -> EquipmentEnumState.GLUTE_MACHINES

        EquipmentEnum.LAT_PULLDOWN -> EquipmentEnumState.LAT_PULLDOWN
        EquipmentEnum.CABLE -> EquipmentEnumState.CABLE
        EquipmentEnum.CABLE_CROSSOVER -> EquipmentEnumState.CABLE_CROSSOVER
        EquipmentEnum.ROW_CABLE -> EquipmentEnumState.ROW_CABLE

        EquipmentEnum.PULL_UP_BAR -> EquipmentEnumState.PULL_UP_BAR
        EquipmentEnum.DIP_BARS -> EquipmentEnumState.DIP_BARS
        EquipmentEnum.ROMAIN_CHAIR -> EquipmentEnumState.ROMAIN_CHAIR
        EquipmentEnum.GLUTE_HAM_RAISE_BENCH -> EquipmentEnumState.GLUTE_HAM_RAISE_BENCH

        EquipmentEnum.FLAT_BENCH -> EquipmentEnumState.FLAT_BENCH
        EquipmentEnum.ADJUSTABLE_BENCH -> EquipmentEnumState.ADJUSTABLE_BENCH
        EquipmentEnum.ADDUCTOR_MACHINE -> EquipmentEnumState.ADDUCTOR_MACHINE
        EquipmentEnum.ABDUCTOR_MACHINE -> EquipmentEnumState.ABDUCTOR_MACHINE
        EquipmentEnum.DECLINE_BENCH -> EquipmentEnumState.DECLINE_BENCH
        EquipmentEnum.FLAT_BENCH_WITH_RACK -> EquipmentEnumState.FLAT_BENCH_WITH_RACK
        EquipmentEnum.INCLINE_BENCH_WITH_RACK -> EquipmentEnumState.INCLINE_BENCH_WITH_RACK
        EquipmentEnum.DECLINE_BENCH_WITH_RACK -> EquipmentEnumState.DECLINE_BENCH_WITH_RACK
        EquipmentEnum.SQUAT_RACK -> EquipmentEnumState.SQUAT_RACK
        EquipmentEnum.PREACHER_CURL_BENCH -> EquipmentEnumState.PREACHER_CURL_BENCH
        EquipmentEnum.ROW_BENCH -> EquipmentEnumState.ROW_BENCH
    }
}