package com.grippo.core.state.examples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.core.state.equipments.stubEquipments
import com.grippo.core.state.filters.FilterValueState
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
        public val filters: ImmutableList<FilterValueState> = ExerciseExampleValueState.filters
    }

    @Composable
    public fun hint(): String? {
        val equipmentTypes: List<EquipmentEnumState> = remember(equipments) {
            equipments
                .map { it.type }
                .distinct()
        }

        if (equipmentTypes.isEmpty()) return null

        val hints: List<String> = equipmentTypes
            .map { it.hint().text() }
            .distinct()

        return hints.joinToString(separator = "\n")
    }
}

public fun stubExerciseExample(): ExerciseExampleState {
    return ExerciseExampleState(
        value = stubExerciseExampleValueState(),
        bundles = stubExerciseExampleBundles(),
        equipments = stubEquipments().random().equipments.take(2).toPersistentList(),
    )
}
