package com.grippo.training.recording.calculation

import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSAreaPoint
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.trainings.ExerciseState

/**
 * Calculator for exercise-related analytics used in charts and progress views.
 *
 * 📌 Responsibilities:
 * - Aggregates raw `ExerciseState` data into chart-ready models (`DSBarData`, `DSAreaData`).
 * - Provides multiple perspectives on training load: total volume, relative intensity,
 *   progression within a workout, and estimated one-rep max (1RM).
 *
 * ⚠️ Notes:
 * - Formulas rely on iteration fields: `volume.value` (weight × reps) and `repetitions.value`.
 * - Brzycki 1RM formula is only valid for sets with ≤ 12 reps.
 * - Intensity comparisons are more meaningful **within the same exercise/muscle group**.
 */
internal class ExerciseAnalyticsCalculator(
    private val colorProvider: ColorProvider
) {
    /**
     * 📊 Exercise Volume Chart
     *
     * - **Definition**: total workload (tonnage) per exercise.
     *   ```
     *   volumeExercise = Σ (iteration.volume)
     *   ```
     * - **X-axis**: exercise name.
     * - **Y-axis**: total volume (kg).
     * - **Use case**: shows which exercises contribute the most load to the workout.
     *
     * Example:
     * - Bench Press: (100×5 + 90×8) = 1220 kg
     * - Pull-Ups: (80×10) = 800 kg
     */
    suspend fun calculateExerciseVolumeChart(exercises: List<ExerciseState>): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.charts.categorical.palette1

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
     * 📊 Exercise Intensity Distribution
     *
     * - **Definition**: average load per repetition for each exercise.
     *   ```
     *   intensity = totalVolume / totalReps
     *   ```
     * - Bars colored by semantic zones (low / medium / high intensity).
     * - **Use case**: useful for comparing different *sets of the same exercise*,
     *   or exercises within the same muscle group.
     *
     * ⚠️ Caution: absolute intensity values across unrelated exercises
     *   (e.g. biceps curl vs squat) are not directly comparable.
     *
     * Example:
     * - Bench Press: 1220 volume / 13 reps ≈ 94 kg
     * - Pull-Ups: 800 volume / 10 reps = 80 kg
     */
    suspend fun calculateIntensityDistribution(exercises: List<ExerciseState>): DSBarData {
        val colors = colorProvider.get()

        val items = exercises.mapIndexedNotNull { index, exercise ->
            val avgIntensity = exercise.iterations.mapNotNull { it.volume.value }
                .let { volumes ->
                    if (volumes.isNotEmpty()) {
                        val totalVolume = volumes.sum()
                        val totalReps = exercise.iterations.sumOf { it.repetitions.value ?: 0 }
                        if (totalReps > 0) totalVolume / totalReps else 0f
                    } else 0f
                }

            val intensityState = IntensityFormatState.of(avgIntensity)

            val color = when (intensityState.average() ?: return@mapIndexedNotNull null) {
                IntensityFormatState.Average.LOW -> colors.semantic.error
                IntensityFormatState.Average.MEDIUM -> colors.semantic.warning
                IntensityFormatState.Average.LARGE -> colors.semantic.success
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
     * - **Definition**: how the average load per rep changes
     *   across exercises in workout order.
     *   ```
     *   avgWeightExercise = Σ(volume) / Σ(reps)
     *   ```
     * - **X-axis**: exercise sequence (Ex1, Ex2, ...).
     * - **Y-axis**: average load per repetition (kg).
     * - **Use case**: visualizes whether load decreases (fatigue),
     *   stays stable, or increases (heavy exercises at the end).
     *
     * ⚠️ This is sequence-based, not real-time.
     *
     * Example:
     * - Ex1 Bench Press ≈ 94 kg
     * - Ex2 Pull-Ups ≈ 80 kg
     * 👉 curve slopes down → intensity dropped later in the workout.
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
                xLabel = exercise.exerciseExample?.name ?: "-"
            )
        }

        return DSAreaData(points = points)
    }

    /**
     * 🏋 Estimated One-Rep Max (1RM)
     *
     * - **Definition**: maximum predicted weight for a single rep,
     *   based on the Brzycki formula:
     *   ```
     *   1RM = weight / (1.0278 - 0.0278 × reps)
     *   ```
     * - For each exercise, takes the maximum 1RM value across all sets.
     * - Colors assigned from categorical palette.
     * - **Use case**: safe way to monitor strength progress without testing true 1RM.
     *
     * ⚠️ Valid only for reps ≤ 12. Accuracy decreases with higher reps.
     *
     * Example:
     * - 100 kg × 5 reps → 1RM ≈ 112 kg
     * - 90 kg × 10 reps → 1RM ≈ 120 kg
     * 👉 Predicted 1RM for Bench Press = 120 kg.
     */
    suspend fun calculateEstimated1RM(exercises: List<ExerciseState>): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.charts.categorical.palette2

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