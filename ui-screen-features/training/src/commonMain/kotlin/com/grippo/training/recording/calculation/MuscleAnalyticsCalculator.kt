package com.grippo.training.recording.calculation

import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSProgressItem
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.muscles.MuscleEnumState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import com.grippo.state.trainings.ExerciseState

internal class MuscleAnalyticsCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider,
) {
    private companion object {
        private const val MAX_VISIBLE_ITEMS = 8
    }

    enum class Mode {
        ABSOLUTE,   // show raw kg
        RELATIVE    // normalize to % of max
    }

    /**
     * 📊 Calculates distribution of training load across muscles or muscle groups.
     *
     * - **Step 1**: compute exercise volume = `sum(iteration.volume)`.
     * - **Step 2**: distribute this volume across muscles via [ExerciseExampleBundleState.percentage].
     * - **Step 3**: optionally aggregate by [MuscleGroupState] (Chest, Legs, etc.).
     * - **Step 4**: normalize values (ABSOLUTE = kg, RELATIVE = % of max).
     *
     * @param exercises performed exercises with iterations
     * @param examples reference list of [ExerciseExampleState] with bundles (muscle involvement)
     * @param groups optional grouping of muscles into regions
     * @param mode output mode (absolute kg or relative %)
     */
    suspend fun calculateMuscleLoadDistribution(
        exercises: List<ExerciseState>,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
        mode: Mode = Mode.RELATIVE
    ): DSProgressData {
        val colors = colorProvider.get()
        val palette = colors.charts.progress.palette

        // Lookup table: exerciseExample.id → ExerciseExampleState
        val exampleMap = examples.associateBy { it.value.id }

        // Step 1: collect load per muscle
        val muscleLoads = mutableMapOf<MuscleEnumState, Float>()
        exercises.forEach { exercise ->
            val volume = exercise.iterations
                .sumOf { it.volume.value?.toDouble() ?: 0.0 }
                .toFloat()

            val exampleId = exercise.exerciseExample?.id ?: return@forEach
            val example = exampleMap[exampleId] ?: return@forEach

            example.bundles.forEach { bundle ->
                val percentage = bundle.percentage.value ?: 0
                if (percentage > 0) {
                    val load = volume * (percentage / 100f)
                    muscleLoads[bundle.muscle.type] =
                        (muscleLoads[bundle.muscle.type] ?: 0f) + load
                }
            }
        }

        if (muscleLoads.isEmpty()) return DSProgressData(items = emptyList())

        // Step 2: aggregate into groups or use muscles directly
        val data: Map<String, Float> = if (groups.isNotEmpty()) {
            groups.associate { group ->
                val groupLoad = group.muscles.fold(0f) { acc, m ->
                    acc + (muscleLoads[m.value.type] ?: 0f)
                }
                group.type.name to groupLoad
            }
        } else {
            muscleLoads.mapKeys { (muscle, _) -> muscle.title().text(stringProvider) }
        }

        // Step 3: normalize
        val maxLoad = data.values.maxOrNull() ?: 1f

        val items = data.entries
            .sortedByDescending { it.value }
            .take(MAX_VISIBLE_ITEMS)
            .mapIndexed { index, (label, load) ->
                val value = when (mode) {
                    Mode.ABSOLUTE -> load
                    Mode.RELATIVE -> ((load / maxLoad) * 100f)
                }
                DSProgressItem(
                    label = label,
                    value = value,
                    color = palette[index % palette.size]
                )
            }

        return DSProgressData(
            items = items,
            valueUnit = when (mode) {
                Mode.ABSOLUTE -> "kg"
                Mode.RELATIVE -> "%"
            }
        )
    }
}