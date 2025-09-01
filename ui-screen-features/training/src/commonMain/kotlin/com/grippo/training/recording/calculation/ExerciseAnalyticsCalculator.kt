package com.grippo.training.recording.calculation

import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSAreaPoint
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSProgressItem
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.trainings.ExerciseState

/**
 * Calculator for different types of exercise analytics used in charts and progress views.
 *
 * 📌 Responsibilities:
 * - Aggregates raw `ExerciseState` data into chart-friendly models (`DSBarData`, `DSAreaData`, `DSProgressData`).
 * - Provides multiple perspectives: volume, intensity, progression, weak points, estimated 1RM.
 *
 * ⚠️ Notes:
 * - Name truncation (`take(8)` / `take(10)`) is UI-specific and may be better handled in the presentation layer.
 * - Some formulas (e.g., Brzycki for 1RM) have inherent domain limitations (e.g., reps ≤ 12).
 */
internal class ExerciseAnalyticsCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider
) {
    /**
     * 📊 Builds a bar chart of exercise volume.
     *
     * - **Volume**: sum of all iteration volumes (`sum(iteration.volume)`).
     * - Each bar = one exercise.
     * - Color palette = categorical.
     *
     * @param exercises list of exercises with iterations.
     * @return [DSBarData] with volume distribution.
     */
    suspend fun calculateExerciseVolumeChart(exercises: List<ExerciseState>): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.charts.categorical.palette

        val items = exercises.mapIndexed { index, exercise ->
            val volume = exercise.iterations.sumOf { it.volume.value?.toDouble() ?: 0.0 }.toFloat()
            DSBarItem(
                label = exercise.name,
                value = volume,
                color = palette[index % palette.size]
            )
        }

        return DSBarData(items = items)
    }

    /**
     * 📊 Builds a bar chart of average intensity per exercise.
     *
     * - **Intensity formula**: total volume / total reps.
     * - Bars colored by intensity zone (light/medium/high).
     * - Labels are truncated (`take(8)`).
     *
     * @param exercises list of exercises with iterations.
     * @return [DSBarData] showing relative exercise intensity.
     */
    suspend fun calculateIntensityDistribution(exercises: List<ExerciseState>): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.charts.categorical.palette

        val items = exercises.mapIndexed { index, exercise ->
            // Calculate average intensity = totalVolume / totalReps
            val avgIntensity = exercise.iterations.mapNotNull { it.volume.value }
                .let { volumes ->
                    if (volumes.isNotEmpty()) {
                        val totalVolume = volumes.sum()
                        val totalReps = exercise.iterations.sumOf { it.repetitions.value ?: 0 }
                        if (totalReps > 0) totalVolume / totalReps else 0f
                    } else 0f
                }

            // Wrap in IntensityFormatState
            val intensityState = IntensityFormatState.of(avgIntensity)

            // Map enum → color
            val color = when (intensityState.average()) {
                IntensityFormatState.Average.LOW -> palette[0]     // low load
                IntensityFormatState.Average.MEDIUM -> palette[1]  // medium load
                IntensityFormatState.Average.LARGE -> palette[2]   // high load
                null -> palette[index % palette.size]              // fallback
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
     * 📈 Builds an area chart representing average load progression across exercises in a workout.
     *
     * - **X-axis**: sequential exercise index (Ex1, Ex2, ...).
     * - **Y-axis**: average load per exercise (`sum(volumes) / totalReps`).
     * - Used to visualize how the relative load changes across workout order.
     *
     * ⚠️ Note: This is not time-based progression, but based on exercise sequence.
     *
     * @param exercises list of performed exercises with iterations.
     * @return [DSAreaData] containing chart points.
     */
    suspend fun calculateIntraWorkoutProgression(exercises: List<ExerciseState>): DSAreaData {
        val points = exercises.mapIndexed { index, exercise ->
            val avgWeight = exercise.iterations.mapNotNull { it.volume.value }.let { volumes ->
                val totalReps = exercise.iterations.sumOf { it.repetitions.value ?: 0 }
                if (volumes.isNotEmpty() && totalReps > 0) {
                    volumes.sum() / totalReps
                } else 0f
            }

            DSAreaPoint(
                x = index.toFloat(),
                y = avgWeight,
                xLabel = "Ex${index + 1}"
            )
        }

        return DSAreaData(points = points)
    }

    /**
     * 📉 Identifies weakest exercises by average intensity and builds a progress chart.
     *
     * - **Intensity formula**: `volume / reps` for each iteration, averaged per exercise.
     * - Exercises sorted ascending by intensity.
     * - Top 5 weakest exercises are selected (`take(5)`).
     * - Values scaled ×2 for better visual separation in progress bars.
     *
     * @param exercises list of performed exercises with iterations.
     * @return [DSProgressData] with weakest exercises for progress visualization.
     */
    suspend fun calculateWeakPoints(exercises: List<ExerciseState>): DSProgressData {
        val colors = colorProvider.get()
        val palette = colors.charts.progress.palette

        val items = exercises.map { exercise ->
            val avgIntensity = exercise.iterations.mapNotNull { iteration ->
                val reps = iteration.repetitions.value ?: 0
                val volume = iteration.volume.value ?: 0f
                if (reps > 0) volume / reps else null
            }.let { weights ->
                if (weights.isNotEmpty()) weights.average().toFloat() else 0f
            }

            exercise to avgIntensity
        }
            .sortedBy { it.second }
            .take(5) // minimal intensity
            .mapIndexed { index, (exercise, intensity) ->
                DSProgressItem(
                    label = exercise.name,
                    value = intensity * 2, // x2 to visualize
                    color = palette[index % palette.size]
                )
            }

        return DSProgressData(items = items)
    }

    /**
     * 🏋 Estimates one-repetition maximum (1RM) for each exercise using Brzycki formula.
     *
     * Formula:
     * ```
     * 1RM = weight / (1.0278 - 0.0278 × reps)
     * ```
     * - `weight` here is the load used in the set (`iteration.volume.value`).
     * - Only valid for reps ≤ 12 (beyond this, accuracy degrades).
     * - For each exercise, the maximum estimated 1RM across all sets is taken.
     * - Colors assigned from categorical palette.
     *
     * @param exercises list of performed exercises with iterations.
     * @return [DSBarData] with estimated 1RM values per exercise.
     */
    suspend fun calculateEstimated1RM(exercises: List<ExerciseState>): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.charts.categorical.palette

        val items = exercises.mapIndexed { index, exercise ->
            val estimated1RM = exercise.iterations.mapNotNull { iteration ->
                val weight = iteration.volume.value ?: 0f
                val reps = iteration.repetitions.value ?: 0
                if (weight > 0 && reps > 0 && reps <= 12) {
                    weight / (1.0278f - 0.0278f * reps)
                } else null
            }.maxOrNull() ?: 0f

            DSBarItem(
                label = exercise.name,
                value = estimated1RM,
                color = palette[index % palette.size]
            )
        }

        return DSBarData(items = items)
    }
}