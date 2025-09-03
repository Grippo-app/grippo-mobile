package com.grippo.calculation

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
        ABSOLUTE,   // raw workload in the same units as iteration.volume (e.g., kg·rep)
        RELATIVE    // normalized to percentage (see relativeMode)
    }

    // Controls how RELATIVE normalization is performed
    public enum class RelativeMode {
        MAX,   // value / max(value) * 100  → heatmap-style
        SUM    // value / sum(value) * 100  → proper share/distribution
    }

    // Optional workload model (defaults to Volume to preserve current semantics)
    public sealed interface Workload {
        public data object Volume : Workload                  // Σ(volume)
        public data object Reps : Workload                    // Σ(reps)
        public data class TUT(val secPerRep: Float = 3f) : Workload // Σ(reps) * secPerRep
    }

    /**
     * 📊 Muscle/Muscle-Group Load Distribution
     *
     * WHAT:
     * 1) For each exercise, compute its workload (default: Σ(volume), assumed volume = load*reps).
     * 2) Distribute this workload across muscles using Example bundles (percentage-based involvement).
     *    - Per-exercise percentages are normalized over the sum of positive entries to avoid over/under allocation.
     * 3) Optionally aggregate on muscle groups (provided via [groups]).
     * 4) Normalize output:
     *    - ABSOLUTE: raw workload units (e.g., kg·rep, reps, or seconds if TUT).
     *    - RELATIVE: percentage either to MAX or SUM (controlled by [relativeMode]).
     *
     * WHY:
     * - Shows which muscles (or groups) actually received the most work, not just how many exercises were picked.
     *
     * PITFALLS:
     * - If "volume" in your data ≠ load*reps, rename units and be explicit in the UI.
     * - A muscle existing in multiple groups will be double-counted at the group stage (ensure unique partitioning).
     * - Colors by palette may shuffle when sorting by value; prefer stable per-muscle colors if you have them.
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
        val palette = colors.charts.progress.palette

        // Build a stable lookup: exampleId -> ExerciseExampleState
        val exampleMap = examples.associateBy { it.value.id }

        // Accumulator keyed by muscle enum to avoid label-collisions between locales
        val muscleLoadMap = mutableMapOf<MuscleEnumState, Float>()

        // ---- Step 1 & 2: per-exercise workload and distribution over muscles (normalized per exercise) ----
        exercises.forEach { ex ->
            // Resolve example id safely: allow both direct id or nested value.id
            val exampleId = ex.exerciseExample?.id ?: ex.exerciseExample?.id ?: return@forEach
            val example = exampleMap[exampleId] ?: return@forEach

            // Compute exercise-level workload
            val exWorkload: Float = when (workload) {
                Workload.Volume -> {
                    ex.iterations.fold(0f) { acc, itn -> acc + (itn.volume.value ?: 0f) }
                }

                Workload.Reps -> {
                    ex.iterations.fold(0) { acc, itn -> acc + (itn.repetitions.value ?: 0) }
                        .toFloat()
                }

                is Workload.TUT -> {
                    val reps =
                        ex.iterations.fold(0) { acc, itn -> acc + (itn.repetitions.value ?: 0) }
                    (reps * workload.secPerRep)
                }
            }.coerceAtLeast(0f)

            if (exWorkload <= 0f) return@forEach

            // Normalize bundle percentages per exercise to avoid over/under allocation.
            // Only positive percentages contribute; zero/negative are ignored.
            var denom = 0f
            example.bundles.forEach { b ->
                val p = (b.percentage.value ?: 0).coerceAtLeast(0)
                denom += p.toFloat()
            }
            if (denom <= 0f) return@forEach

            example.bundles.forEach { b ->
                val p = (b.percentage.value ?: 0).coerceAtLeast(0)
                if (p == 0) return@forEach
                val share = p / denom // 0..1 per exercise
                val loadShare = exWorkload * share
                val muscle = b.muscle.type
                muscleLoadMap[muscle] = (muscleLoadMap[muscle] ?: 0f) + loadShare
            }
        }

        if (muscleLoadMap.isEmpty()) {
            return DSProgressData(items = emptyList())
        }

        // ---- Step 3: aggregate to groups if provided, else stay at muscle level ----
        // Keep internal map keyed by enum; convert to labels at the very end.
        val labelValueMap: Map<String, Float> = if (groups.isNotEmpty()) {
            // Aggregate muscles into their groups; if a muscle appears in multiple groups, it will be double-counted.
            groups.map { group ->
                val sum = group.muscles.fold(0f) { acc, m ->
                    acc + (muscleLoadMap[m.value.type] ?: 0f)
                }
                // Use localized group label
                group.type.title().text(stringProvider) to sum
            }.filter { it.second > 0f }
                .toMap()
        } else {
            // Muscles directly: convert enum to localized label
            muscleLoadMap.mapKeys { (muscle, _) -> muscle.title().text(stringProvider) }
        }

        if (labelValueMap.isEmpty()) {
            return DSProgressData(items = emptyList())
        }

        // ---- Step 4: normalization (ABSOLUTE or RELATIVE with mode) ----
        val values = labelValueMap.values
        val maxVal = values.maxOrNull() ?: 1f
        val sumVal = values.sum().takeIf { it > 0f } ?: 1f

        val items = labelValueMap.entries
            .sortedByDescending { it.value }
            .mapIndexed { index, (label, raw) ->
                val display = when (mode) {
                    Mode.ABSOLUTE -> raw
                    Mode.RELATIVE -> when (relativeMode) {
                        RelativeMode.MAX -> (raw / maxVal) * 100f
                        RelativeMode.SUM -> (raw / sumVal) * 100f
                    }
                }
                DSProgressItem(
                    label = label,
                    value = display,
                    color = palette[index % palette.size] // Prefer stable per-muscle colors if available
                )
            }

        return DSProgressData(items = items)
    }
}