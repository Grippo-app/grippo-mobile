package com.grippo.calculation

import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSAreaPoint
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.state.exercise.examples.WeightTypeEnumState
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState

/**
 * 📊 **Training Analytics Calculator**
 *
 * Provides multiple analytic charts from training data:
 *
 * - 📦 Exercise Volume (Σ weight × reps)
 * - 📏 Exercise Intensity (avg load per rep)
 * - 📈 Intra-Workout Progression (trend across session order)
 * - 🏋 Estimated 1RM (Brzycki formula)
 *
 * ---
 * 🔎 **Levels of input**
 * - `ExerciseState` list → single training
 * - `TrainingState` list → multiple trainings aggregated
 *
 * ⚠️ **Assumptions**
 * - `iteration.volume.value` stores `weight × reps`
 * - Exercises are ordered by execution for progression charts
 * - 1RM is only estimated for non-bodyweight exercises
 */
public class TrainingAnalyticsCalculator(
    private val colorProvider: ColorProvider
) {

    // ---------------- Exercise Volume ----------------

    /** 📦 Exercise Volume Chart — Σ(weight × reps) per exercise */
    public suspend fun calculateExerciseVolumeChartFromExercises(
        exercises: List<ExerciseState>
    ): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.palette.palette9Blue

        val items = exercises.mapIndexed { index, ex ->
            val tonnage = ex.iterations.fold(0f) { acc, it ->
                val w = it.volume.value ?: 0f
                val r = it.repetitions.value ?: 0
                acc + (w * r)
            }.coerceAtLeast(0f)

            DSBarItem(
                label = ex.name,
                value = tonnage,
                color = palette[index % palette.size]
            )
        }

        return DSBarData(items = items)
    }

    /** 📦 Exercise Volume Chart across multiple trainings */
    public suspend fun calculateExerciseVolumeChartFromTrainings(
        trainings: List<TrainingState>
    ): DSBarData = calculateExerciseVolumeChartFromExercises(trainings.flatMap { it.exercises })

    // ---------------- Exercise Intensity ----------------

    /** 📏 Exercise Intensity Distribution — avg load per rep */
    public suspend fun calculateIntensityDistributionFromExercises(
        exercises: List<ExerciseState>
    ): DSBarData {
        val colors = colorProvider.get()

        val items = exercises.mapNotNull { ex ->
            var sumTonnage = 0f // Σ(weight * reps)
            var sumReps = 0     // Σ(reps)
            ex.iterations.forEach { itn ->
                val w = itn.volume.value
                val r = itn.repetitions.value
                if (w != null && r != null && r > 0) {
                    sumTonnage += w * r
                    sumReps += r
                }
            }
            if (sumReps <= 0) return@mapNotNull null

            val avgIntensity = (sumTonnage / sumReps).coerceAtLeast(0f)
            val state = IntensityFormatState.of(avgIntensity)
            val color = when (state.average() ?: return@mapNotNull null) {
                IntensityFormatState.Average.LOW -> colors.semantic.success
                IntensityFormatState.Average.MEDIUM -> colors.semantic.warning
                IntensityFormatState.Average.LARGE -> colors.semantic.error
            }
            DSBarItem(label = ex.name, value = avgIntensity, color = color)
        }

        return DSBarData(items = items)
    }

    /** 📏 Intensity Distribution across multiple trainings */
    public suspend fun calculateIntensityDistributionFromTrainings(
        trainings: List<TrainingState>
    ): DSBarData = calculateIntensityDistributionFromExercises(trainings.flatMap { it.exercises })

    // ---------------- Intra-Workout Progression ----------------

    /** 📈 Intra-Workout Load Progression */
    public fun calculateIntraProgressionFromExercises(
        exercises: List<ExerciseState>
    ): DSAreaData {
        val points = exercises.mapIndexed { index, ex ->
            var sumTonnage = 0f
            var sumReps = 0
            ex.iterations.forEach { itn ->
                val w = itn.volume.value
                val r = itn.repetitions.value
                if (w != null && r != null && r > 0) {
                    sumTonnage += w * r
                    sumReps += r
                }
            }
            val avgWeight = if (sumReps > 0) (sumTonnage / sumReps) else 0f
            DSAreaPoint(x = index.toFloat(), y = avgWeight.coerceAtLeast(0f), xLabel = ex.name)
        }
        return DSAreaData(points = points)
    }

    /** 📈 Intra-Workout Progression across multiple trainings */
    public fun calculateIntraProgressionFromTrainings(
        trainings: List<TrainingState>
    ): DSAreaData = calculateIntraProgressionFromExercises(trainings.flatMap { it.exercises })

    // ---------------- Estimated 1RM ----------------

    /** 🏋 Estimated One-Rep Max (Brzycki) */
    public suspend fun calculateEstimated1RMFromExercises(
        exercises: List<ExerciseState>
    ): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.palette.palette18Colorful

        val items = exercises.mapIndexed { index, ex ->
            val isBodyweight = ex.exerciseExample?.weightType == WeightTypeEnumState.BODY_WEIGHT

            val best1RM = if (isBodyweight) null else {
                var max1rm = Float.NEGATIVE_INFINITY
                ex.iterations.forEach { itn ->
                    val reps = itn.repetitions.value ?: 0
                    val weight = itn.volume.value
                    if (weight != null && reps in 1..12) {
                        val denom = (1.0278f - 0.0278f * reps)
                        if (weight > 0f && denom > 0f) {
                            val e1rm = weight / denom
                            if (e1rm > max1rm) max1rm = e1rm
                        }
                    }
                }
                if (max1rm.isFinite()) max1rm else null
            }

            DSBarItem(
                label = ex.name,
                value = (best1RM ?: 0f).coerceAtLeast(0f),
                color = palette[index % palette.size]
            )
        }

        return DSBarData(items = items)
    }

    /** 🏋 Estimated 1RM across multiple trainings */
    public suspend fun calculateEstimated1RMFromTrainings(
        trainings: List<TrainingState>
    ): DSBarData = calculateEstimated1RMFromExercises(trainings.flatMap { it.exercises })
}
