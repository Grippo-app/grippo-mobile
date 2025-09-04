package com.grippo.calculation

import androidx.compose.ui.graphics.Color
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSProgressItem
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.muscles.MuscleEnumState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import com.grippo.state.trainings.ExerciseState

public class TrainingMusclesCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider,
) {

    public enum class Mode {
        ABSOLUTE,
        RELATIVE
    }

    public enum class RelativeMode {
        MAX,
        SUM
    }

    public sealed interface Workload {
        public data object Volume : Workload
        public data object Reps : Workload
        public data class TUT(val secPerRep: Float = 3f) : Workload
    }

    /**
     * 📊 Muscle/Muscle-Group Load Distribution with heatmap coloring
     */
    public suspend fun calculateMuscleLoadDistribution(
        exercises: List<ExerciseState>,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
        mode: Mode,
        relativeMode: RelativeMode,
        workload: Workload,
    ): DSProgressData {
        val colors = colorProvider.get()
        val scaleStops = colors.muscle.scaleStops.sortedBy { it.first }

        val exampleMap = examples.associateBy { it.value.id }
        val muscleLoadMap = mutableMapOf<MuscleEnumState, Float>()

        // ---- Step 1 & 2 ----
        exercises.forEach { ex ->
            val exampleId = ex.exerciseExample?.id ?: return@forEach
            val example = exampleMap[exampleId] ?: return@forEach

            val exWorkload: Float = when (workload) {
                Workload.Volume -> ex.iterations.fold(0f) { acc, itn ->
                    acc + (itn.volume.value ?: 0f)
                }

                Workload.Reps -> ex.iterations.fold(0) { acc, itn ->
                    acc + (itn.repetitions.value ?: 0)
                }.toFloat()

                is Workload.TUT -> {
                    val reps =
                        ex.iterations.fold(0) { acc, itn -> acc + (itn.repetitions.value ?: 0) }
                    reps * workload.secPerRep
                }
            }.coerceAtLeast(0f)

            if (exWorkload <= 0f) return@forEach

            var denom = 0f
            example.bundles.forEach { b ->
                denom += (b.percentage.value ?: 0).coerceAtLeast(0).toFloat()
            }
            if (denom <= 0f) return@forEach

            example.bundles.forEach { b ->
                val p = (b.percentage.value ?: 0).coerceAtLeast(0)
                if (p == 0) return@forEach
                val share = p / denom
                val loadShare = exWorkload * share
                val muscle = b.muscle.type
                muscleLoadMap[muscle] = (muscleLoadMap[muscle] ?: 0f) + loadShare
            }
        }

        if (muscleLoadMap.isEmpty()) {
            return DSProgressData(items = emptyList())
        }

        // ---- Step 3 ----
        val labelValueMap: Map<String, Float> = if (groups.isNotEmpty()) {
            groups.map { group ->
                val sum =
                    group.muscles.fold(0f) { acc, m -> acc + (muscleLoadMap[m.value.type] ?: 0f) }
                group.type.title().text(stringProvider) to sum
            }.filter { it.second > 0f }.toMap()
        } else {
            muscleLoadMap.mapKeys { (muscle, _) -> muscle.title().text(stringProvider) }
        }

        if (labelValueMap.isEmpty()) {
            return DSProgressData(items = emptyList())
        }

        // ---- Step 4 ----
        val values = labelValueMap.values
        val maxVal = values.maxOrNull() ?: 1f
        val sumVal = values.sum().takeIf { it > 0f } ?: 1f

        // ---- Step 5: heatmap coloring ----
        val items = labelValueMap.entries
            .sortedByDescending { it.value }
            .map { (label, raw) ->
                val display = when (mode) {
                    Mode.ABSOLUTE -> raw
                    Mode.RELATIVE -> when (relativeMode) {
                        RelativeMode.MAX -> (raw / maxVal) * 100f
                        RelativeMode.SUM -> (raw / sumVal) * 100f
                    }
                }

                val ratio = if (maxVal == 0f) 0f else (raw / maxVal).coerceIn(0f, 1f)
                val color = interpolateColor(ratio, scaleStops)

                DSProgressItem(
                    label = label,
                    value = display,
                    color = color
                )
            }

        return DSProgressData(items = items)
    }

    private fun interpolateColor(ratio: Float, stops: List<Pair<Float, Color>>): Color {
        for (i in 0 until stops.size - 1) {
            val (p1, c1) = stops[i]
            val (p2, c2) = stops[i + 1]
            if (ratio in p1..p2) {
                val t = (ratio - p1) / (p2 - p1)
                return lerpColor(c1, c2, t)
            }
        }
        return stops.last().second
    }

    private fun lerpColor(c1: Color, c2: Color, t: Float): Color {
        return Color(
            red = (c1.red + (c2.red - c1.red) * t),
            green = (c1.green + (c2.green - c1.green) * t),
            blue = (c1.blue + (c2.blue - c1.blue) * t),
            alpha = (c1.alpha + (c2.alpha - c1.alpha) * t),
        )
    }
}