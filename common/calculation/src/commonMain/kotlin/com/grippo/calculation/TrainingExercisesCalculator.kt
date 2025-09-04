package com.grippo.calculation

import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSAreaPoint
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.state.exercise.examples.WeightTypeEnumState
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.trainings.ExerciseState

public class TrainingExercisesCalculator(
    private val colorProvider: ColorProvider
) {

    /**
     * 📊 Exercise Volume Chart
     *
     * WHAT:
     * Total workload (tonnage) per exercise.
     *   volumeExercise = Σ(setVolume), where setVolume is expected to be weight * reps
     *
     * WHY:
     * Shows which exercises contributed the most load.
     *
     * ASSUMPTIONS:
     * - iteration.volume.value holds weight*reps in consistent units (e.g., kg·rep).
     * - Bars are NOT sorted here; sort in UI if needed (e.g., desc by value).
     *
     * PITFALLS:
     * - If your "volume" is defined differently, rename axis/units accordingly.
     * - Summing across very different lifts is fine for load share, not for intensity.
     */
    public suspend fun calculateExerciseVolumeChart(
        exercises: List<ExerciseState>
    ): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.palette.palette9Blue

        val items = exercises.mapIndexed { index, exercise ->
            // Sum volume safely (null -> 0f)
            val totalVolume = exercise.iterations.fold(0f) { acc, it ->
                acc + (it.volume.value ?: 0f)
            }.coerceAtLeast(0f)

            DSBarItem(
                label = exercise.name,
                value = totalVolume,
                color = palette[index % palette.size]
            )
        }

        return DSBarData(items = items)
    }

    /**
     * 📊 Exercise Intensity Distribution
     *
     * WHAT:
     * Average load per repetition for each exercise.
     *   avgIntensity = (Σ(volume_i)) / (Σ(reps_i))  ONLY across sets where volume_i != null AND reps_i > 0
     *
     * WHY:
     * Good to compare sets of the same exercise or within the same muscle group.
     *
     * COLORING:
     * - Low  -> success (green)
     * - Mid  -> warning (yellow)
     * - High -> error (red)
     *
     * PITFALLS:
     * - Absolute kg across unrelated exercises are NOT comparable (e.g., curl vs squat).
     *   Prefer relative metrics (e.g., % of e1RM) if available.
     */
    public suspend fun calculateIntensityDistribution(
        exercises: List<ExerciseState>
    ): DSBarData {
        val colors = colorProvider.get()

        val items = exercises.mapIndexedNotNull { _, exercise ->
            // Single pass accumulation: include set only if both volume and reps are valid
            var sumVolume = 0f
            var sumReps = 0
            exercise.iterations.forEach { itn ->
                val v = itn.volume.value
                val r = itn.repetitions.value
                if (v != null && r != null && r > 0) {
                    sumVolume += v
                    sumReps += r
                }
            }
            if (sumReps <= 0) return@mapIndexedNotNull null

            val avgIntensity = (sumVolume / sumReps).coerceAtLeast(0f)

            val state = IntensityFormatState.of(avgIntensity)
            val color = when (state.average() ?: return@mapIndexedNotNull null) {
                IntensityFormatState.Average.LOW -> colors.semantic.success
                IntensityFormatState.Average.MEDIUM -> colors.semantic.warning
                IntensityFormatState.Average.LARGE -> colors.semantic.error
            }

            DSBarItem(
                label = exercise.name,
                value = avgIntensity,
                color = color
            )
        }

        return DSBarData(items = items)
    }

    /**
     * 📈 Intra-Workout Load Progression
     *
     * WHAT:
     * How the average load per rep changes across the workout sequence.
     *   avgWeight(ex) = (Σ(volume_i)) / (Σ(reps_i)) over valid sets (volume_i != null, reps_i > 0)
     *
     * X:
     * Exercise order in the list. Ensure the input list is ordered by execution time.
     *
     * WHY:
     * Visualizes whether intensity trends down (fatigue), stays stable,
     * or bumps up (heavy lifts placed later).
     *
     * PITFALLS:
     * - Sequence-based, not real-time.
     * - Mixing very different exercises is fine for trend shape, not for absolute compare.
     */
    public fun calculateIntraProgression(
        exercises: List<ExerciseState>
    ): DSAreaData {
        val points = exercises.mapIndexed { index, exercise ->
            var sumVolume = 0f
            var sumReps = 0
            exercise.iterations.forEach { itn ->
                val v = itn.volume.value
                val r = itn.repetitions.value
                if (v != null && r != null && r > 0) {
                    sumVolume += v
                    sumReps += r
                }
            }

            val avgWeight = if (sumReps > 0) (sumVolume / sumReps) else 0f

            DSAreaPoint(
                x = index.toFloat(),
                y = avgWeight.coerceAtLeast(0f),
                xLabel = exercise.name
            )
        }

        return DSAreaData(points = points)
    }

    /**
     * 🏋 Estimated One-Rep Max (1RM) — Brzycki
     *
     * WHAT:
     * Predicted 1RM from reps and load per set using Brzycki:
     *   1RM = load / (1.0278 - 0.0278 * reps), valid for reps in [1..12]
     *
     * HOW:
     * - Because model doesn't expose explicit load, we derive load per set as:
     *     load = setVolume / reps
     *   (only if setVolume != null and reps > 0)
     * - For BODY_WEIGHT exercises we SKIP estimation (needs body mass / added weight).
     * - Per exercise we take MAX 1RM across valid sets.
     *
     * WHY:
     * Track strength without testing true max.
     *
     * PITFALLS:
     * - Reps > 12 are discarded (poor accuracy).
     * - Derived load assumes setVolume = load * reps; if your semantics differ, do not use this.
     * - For bodyweight/bands, 1RM in kg is meaningless without added load and body mass.
     */
    public suspend fun calculateEstimated1RM(
        exercises: List<ExerciseState>
    ): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.palette.palette18Colorful

        val items = exercises.mapIndexed { index, exercise ->
            val isBodyweight =
                exercise.exerciseExample?.weightType == WeightTypeEnumState.BODY_WEIGHT

            val best1RM = if (isBodyweight) {
                // Skip bodyweight-only exercises: no truthful kg 1RM without body mass/added load info
                null
            } else {
                var max1rm = Float.NEGATIVE_INFINITY
                exercise.iterations.forEach { itn ->
                    val reps = itn.repetitions.value ?: 0
                    val volume = itn.volume.value
                    if (volume != null && reps in 1..12) {
                        val load = volume / reps // derive per-set load
                        val denom = (1.0278f - 0.0278f * reps)
                        if (load > 0f && denom > 0f) {
                            val e1rm = load / denom
                            if (e1rm > max1rm) max1rm = e1rm
                        }
                    }
                }
                if (max1rm.isFinite()) max1rm else null
            }

            DSBarItem(
                label = exercise.name,
                value = (best1RM ?: 0f).coerceAtLeast(0f),
                color = palette[index % palette.size]
            )
        }

        return DSBarData(items = items)
    }
}
