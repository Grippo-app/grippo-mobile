package com.grippo.core.state.examples

import androidx.compose.runtime.Immutable
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.core.state.equipments.stubEquipments
import com.grippo.core.state.filters.FilterValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public data class ExerciseExampleState(
    val value: ExerciseExampleValueState,
    val bundles: ImmutableList<ExerciseExampleBundleState>,
    val equipments: ImmutableList<EquipmentState>,
) {
    public companion object {
        public val filters: ImmutableList<FilterValue> = ExerciseExampleValueState.filters
    }
}

public fun stubExerciseExample(): ExerciseExampleState {
    return ExerciseExampleState(
        value = stubExerciseExampleValueState(),
        bundles = stubExerciseExampleBundles(),
        equipments = stubEquipments().random().equipments.take(2).toPersistentList(),
    )
}
