package com.grippo.core.state.examples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.grippo.core.state.ImmutableListSerializer
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.core.state.equipments.stubEquipments
import com.grippo.core.state.formatters.UiText
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.equipment_hint_common
import com.grippo.design.resources.provider.equipment_hint_none
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
) {
    @Composable
    public fun hint(): String? {
        val equipmentTypes: List<EquipmentEnumState> = remember(equipments) {
            equipments
                .map { it.type }
                .distinct()
        }

        if (equipmentTypes.isEmpty()) {
            return UiText.Res(Res.string.equipment_hint_none).text()
        }

        val baseCableTypes = setOf(
            EquipmentEnumState.CABLE_CROSSOVER,
            EquipmentEnumState.CABLE,
            EquipmentEnumState.LAT_PULLDOWN,
            EquipmentEnumState.ROW_CABLE,
        )

        val attachmentTypes = setOf(
            EquipmentEnumState.ROPE,
            EquipmentEnumState.STRAIGHT_BAR,
            EquipmentEnumState.V_BAR,
            EquipmentEnumState.WIDE_GRIP_HANDLE,
            EquipmentEnumState.CLOSE_GRIP_HANDLE,
            EquipmentEnumState.CORD_HANDLES,
        )

        val placeTypes = setOf(
            EquipmentEnumState.FLAT_BENCH,
            EquipmentEnumState.ADJUSTABLE_BENCH,
            EquipmentEnumState.DECLINE_BENCH,
            EquipmentEnumState.FLAT_BENCH_WITH_RACK,
            EquipmentEnumState.INCLINE_BENCH_WITH_RACK,
            EquipmentEnumState.DECLINE_BENCH_WITH_RACK,
            EquipmentEnumState.SQUAT_RACK,
            EquipmentEnumState.PREACHER_CURL_BENCH,
            EquipmentEnumState.ROW_BENCH,
        )

        val typeSet = equipmentTypes.toSet()

        val hasCrossover = EquipmentEnumState.CABLE_CROSSOVER in typeSet
        val hasRack = EquipmentEnumState.FLAT_BENCH_WITH_RACK in typeSet ||
                EquipmentEnumState.INCLINE_BENCH_WITH_RACK in typeSet ||
                EquipmentEnumState.DECLINE_BENCH_WITH_RACK in typeSet ||
                EquipmentEnumState.SQUAT_RACK in typeSet

        val hasPrimaryWeightSource =
            equipmentTypes.any { it !in attachmentTypes && it !in placeTypes }

        val orderedBaseCables: List<EquipmentEnumState> = equipmentTypes
            .filter { it in baseCableTypes }
            .filterNot { it == EquipmentEnumState.CABLE && hasCrossover }

        val orderedAttachments: List<EquipmentEnumState> = equipmentTypes
            .filter { it in attachmentTypes }

        val orderedRest: List<EquipmentEnumState> = equipmentTypes
            .filter { it !in baseCableTypes && it !in attachmentTypes }
            .filterNot { it == EquipmentEnumState.BARBELL && hasRack }

        val finalOrderedTypes: List<EquipmentEnumState> = buildList {
            if (orderedBaseCables.isNotEmpty()) {
                addAll(orderedBaseCables)
                addAll(orderedAttachments)
                addAll(orderedRest)
            } else {
                addAll(orderedRest)
                addAll(orderedAttachments)
            }
        }

        val orderedLines = LinkedHashSet<String>()

        if (!hasPrimaryWeightSource) {
            orderedLines.add(UiText.Res(Res.string.equipment_hint_common).text())
        }

        finalOrderedTypes.forEach { type ->
            orderedLines.add(type.hint().text())
        }

        val result = orderedLines
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")

        return result.ifBlank { null }
    }
}

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
