package com.grippo.state.exercise.examples

import androidx.compose.runtime.Immutable
import com.grippo.state.equipments.EquipmentState
import com.grippo.state.equipments.stubEquipments
import com.grippo.state.filters.FilterValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class ExerciseExampleState(
    val value: ExerciseExampleValueState,
    val bundles: ImmutableList<ExerciseExampleBundleState>,
    val equipments: ImmutableList<EquipmentState>,
) {
    public companion object {
        public val filters: ImmutableList<FilterValue> = buildList {
            addAll(ExerciseExampleValueState.filters)
        }.toPersistentList()
    }
}

public fun stubExerciseExample(): ExerciseExampleState {
    return ExerciseExampleState(
        value = stubExerciseExampleValueState(),
        bundles = stubExerciseExampleBundles(),
        equipments = stubEquipments().random().equipments.take(2).toPersistentList(),
    )
}