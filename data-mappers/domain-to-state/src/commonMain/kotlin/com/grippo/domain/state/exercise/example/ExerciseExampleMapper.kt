package com.grippo.domain.state.exercise.example

import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.equipment.models.EquipmentEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.domain.state.equipment.toState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<ExerciseExample>.toState(): ImmutableList<ExerciseExampleState> {
    return map { it.toState() }.toPersistentList()
}

public fun ExerciseExample.toState(): ExerciseExampleState {
    return ExerciseExampleState(
        value = value.toState(),
        components = components.toState(),
        bundles = bundles.toState(),
        equipments = equipments
            .sortedWith(compareBy({ it.sortPriority() }, { it.type.name }))
            .toState(),
    )
}

private fun Equipment.sortPriority(): Int {
    return when (this.type) {
        EquipmentEnum.CABLE_CROSSOVER -> 10
        EquipmentEnum.CABLE -> 11
        EquipmentEnum.LAT_PULLDOWN -> 12
        EquipmentEnum.ROW_CABLE -> 13

        EquipmentEnum.DUMBBELLS -> 20
        EquipmentEnum.BARBELL -> 21
        EquipmentEnum.EZ_BAR -> 22
        EquipmentEnum.TRAP_BAR -> 23

        EquipmentEnum.SMITH_MACHINES -> 30
        EquipmentEnum.HACK_SQUAT_MACHINES -> 31
        EquipmentEnum.LEG_PRESS_MACHINE -> 32
        EquipmentEnum.DEADLIFT_MACHINES -> 33
        EquipmentEnum.CHEST_PRESS_MACHINES -> 34
        EquipmentEnum.SHOULDER_PRESS_MACHINES -> 35
        EquipmentEnum.LATERAL_RAISE_MACHINES -> 36
        EquipmentEnum.TRICEPS_MACHINES -> 37
        EquipmentEnum.BICEPS_MACHINES -> 38
        EquipmentEnum.LEG_EXTENSION_MACHINES -> 39
        EquipmentEnum.LEG_CURL_MACHINES -> 40
        EquipmentEnum.CALF_RAISE_MACHINES -> 41
        EquipmentEnum.GLUTE_MACHINES -> 42
        EquipmentEnum.ADDUCTOR_MACHINE -> 43
        EquipmentEnum.ABDUCTOR_MACHINE -> 44
        EquipmentEnum.AB_MACHINES -> 45
        EquipmentEnum.BUTTERFLY -> 46
        EquipmentEnum.BUTTERFLY_REVERSE -> 47

        EquipmentEnum.FLAT_BENCH_WITH_RACK -> 60
        EquipmentEnum.INCLINE_BENCH_WITH_RACK -> 61
        EquipmentEnum.DECLINE_BENCH_WITH_RACK -> 62
        EquipmentEnum.SQUAT_RACK -> 63
        EquipmentEnum.FLAT_BENCH -> 64
        EquipmentEnum.ADJUSTABLE_BENCH -> 65
        EquipmentEnum.DECLINE_BENCH -> 66
        EquipmentEnum.PREACHER_CURL_BENCH -> 67
        EquipmentEnum.ROW_BENCH -> 68

        EquipmentEnum.PULL_UP_BAR -> 80
        EquipmentEnum.DIP_BARS -> 81
        EquipmentEnum.ROMAIN_CHAIR -> 82
        EquipmentEnum.GLUTE_HAM_RAISE_BENCH -> 83

        EquipmentEnum.ROPE -> 100
        EquipmentEnum.STRAIGHT_BAR -> 101
        EquipmentEnum.V_BAR -> 102
        EquipmentEnum.WIDE_GRIP_HANDLE -> 103
        EquipmentEnum.CLOSE_GRIP_HANDLE -> 104
        EquipmentEnum.CORD_HANDLES -> 105
    }
}
