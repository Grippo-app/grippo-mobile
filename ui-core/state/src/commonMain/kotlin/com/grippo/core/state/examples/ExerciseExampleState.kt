package com.grippo.core.state.examples

import androidx.compose.runtime.Immutable
import com.grippo.core.state.ImmutableListSerializer
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.core.state.equipments.stubEquipments
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public data class ExerciseExampleState(
    val value: ExerciseExampleValueState,
    val components: ExerciseExampleComponentsState,
    @Serializable(with = ImmutableListSerializer::class)
    val bundles: ImmutableList<ExerciseExampleBundleState>,
    @Serializable(with = ImmutableListSerializer::class)
    val equipments: ImmutableList<EquipmentState>,
)

public fun stubExerciseExample(): ExerciseExampleState {
    return ExerciseExampleState(
        value = stubExerciseExampleValueState(),
        components = ExerciseExampleComponentsState.BodyAndExtra(
            bodyRequired = true,
            bodyMultiplier = 0.8f,
            extraRequired = false
        ),
        bundles = stubExerciseExampleBundles(),
        equipments = stubEquipments().random().equipments.take(2).toPersistentList(),
    )
}
