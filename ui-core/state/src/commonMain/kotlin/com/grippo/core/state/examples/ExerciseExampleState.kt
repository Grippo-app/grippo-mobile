package com.grippo.core.state.examples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.grippo.core.state.ImmutableListSerializer
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.core.state.equipments.stubEquipments
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.equipment_hint_common
import com.grippo.design.resources.provider.equipment_hint_none
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

// File-level constants — never recreated.
private val BASE_CABLE_TYPES: Set<EquipmentEnumState> = setOf(
    EquipmentEnumState.CABLE_CROSSOVER,
    EquipmentEnumState.CABLE,
    EquipmentEnumState.LAT_PULLDOWN,
    EquipmentEnumState.ROW_CABLE,
)

private val ATTACHMENT_TYPES: Set<EquipmentEnumState> = setOf(
    EquipmentEnumState.ROPE,
    EquipmentEnumState.STRAIGHT_BAR,
    EquipmentEnumState.V_BAR,
    EquipmentEnumState.WIDE_GRIP_HANDLE,
    EquipmentEnumState.CLOSE_GRIP_HANDLE,
    EquipmentEnumState.CORD_HANDLES,
)

private val PLACE_TYPES: Set<EquipmentEnumState> = setOf(
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

private val RACK_TYPES: Set<EquipmentEnumState> = setOf(
    EquipmentEnumState.FLAT_BENCH_WITH_RACK,
    EquipmentEnumState.INCLINE_BENCH_WITH_RACK,
    EquipmentEnumState.DECLINE_BENCH_WITH_RACK,
    EquipmentEnumState.SQUAT_RACK,
)

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

    /**
     * Holder for hint computation result that does NOT touch Composition.
     * [includeCommonLine] tells the @Composable layer whether to prepend
     * the equipment_hint_common string.
     */
    @Immutable
    private data class HintPlan(
        val includeCommonLine: Boolean,
        val orderedHintRes: ImmutableList<StringResource>,
    )

    @Composable
    public fun hint(): String? {
        // Preserve original semantics: only the explicit "no equipment" hint
        // when the input list itself is empty.
        if (equipments.isEmpty()) {
            return UiText.Res(Res.string.equipment_hint_none).text()
        }

        val plan = remember(equipments) { computeHintPlan(equipments) }
        val strings = AppTokens.strings

        // Build the final string outside remember (strings.res is @Composable).
        val sb = StringBuilder()
        val seen = HashSet<String>()
        if (plan.includeCommonLine) {
            val s = strings.res(Res.string.equipment_hint_common)
            if (s.isNotBlank() && seen.add(s)) sb.append(s)
        }
        plan.orderedHintRes.forEach { res ->
            val s = strings.res(res)
            if (s.isNotBlank() && seen.add(s)) {
                if (sb.isNotEmpty()) sb.append('\n')
                sb.append(s)
            }
        }
        val result = sb.toString()
        return result.ifBlank { null }
    }

    private companion object {
        /**
         * Pure (non-composable) computation: figures out which equipment hints
         * should be shown, in what order, and whether to prepend the common line.
         */
        fun computeHintPlan(equipments: ImmutableList<EquipmentState>): HintPlan {
            if (equipments.isEmpty()) {
                return HintPlan(
                    includeCommonLine = false,
                    orderedHintRes = persistentListOf(),
                )
            }

            val equipmentTypes: List<EquipmentEnumState> = equipments
                .map { it.type }
                .distinct()

            val typeSet: Set<EquipmentEnumState> = equipmentTypes.toHashSet()

            val hasCrossover = EquipmentEnumState.CABLE_CROSSOVER in typeSet
            val hasRack = typeSet.any { it in RACK_TYPES }

            val hasPrimaryWeightSource =
                equipmentTypes.any { it !in ATTACHMENT_TYPES && it !in PLACE_TYPES }

            val orderedBaseCables: List<EquipmentEnumState> = equipmentTypes
                .filter { it in BASE_CABLE_TYPES }
                .filterNot { it == EquipmentEnumState.CABLE && hasCrossover }

            val orderedAttachments: List<EquipmentEnumState> = equipmentTypes
                .filter { it in ATTACHMENT_TYPES }

            val orderedRest: List<EquipmentEnumState> = equipmentTypes
                .filter { it !in BASE_CABLE_TYPES && it !in ATTACHMENT_TYPES }
                .filterNot { it == EquipmentEnumState.BARBELL && hasRack }

            val finalOrderedTypes: List<EquipmentEnumState> = if (orderedBaseCables.isNotEmpty()) {
                buildList {
                    addAll(orderedBaseCables)
                    addAll(orderedAttachments)
                    addAll(orderedRest)
                }
            } else {
                buildList {
                    addAll(orderedRest)
                    addAll(orderedAttachments)
                }
            }

            val orderedHintRes: ImmutableList<StringResource> = finalOrderedTypes
                .map { it.hintRes() }
                .toPersistentList()

            return HintPlan(
                includeCommonLine = !hasPrimaryWeightSource,
                orderedHintRes = orderedHintRes,
            )
        }
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
