package com.grippo.calculation

import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.IterationState
import com.grippo.state.trainings.TrainingMetrics
import com.grippo.state.trainings.TrainingState

public class MetricsAggregator {

    /**
     * Calculate metrics from raw iterations.
     *
     * Valid set: weight != null AND reps != null AND reps > 0
     * Volume (tonnage)   = Σ(weight * reps) over valid sets
     * Repetitions        = Σ(reps)          over valid sets
     * Intensity (kg/rep) = Volume / Repetitions
     */
    public fun calculateIterations(iterations: List<IterationState>): TrainingMetrics {
        var sumTonnage = 0.0   // Σ(weight * reps) over valid sets
        var sumReps = 0        // Σ(reps)          over valid sets

        iterations.forEach { itn ->
            val weight = (itn.volume as? VolumeFormatState.Valid)?.value
            val reps = (itn.repetitions as? RepetitionsFormatState.Valid)?.value
            if (weight != null && reps != null && reps > 0) {
                sumTonnage += weight.toDouble() * reps.toDouble()
                sumReps += reps
            }
        }

        val totalVolume = sumTonnage.toFloat().coerceAtLeast(0f)
        val totalReps = sumReps.coerceAtLeast(0)
        val avgIntensity = if (totalReps > 0) totalVolume / totalReps else 0f

        return TrainingMetrics(
            volume = VolumeFormatState.of(totalVolume),
            repetitions = RepetitionsFormatState.of(totalReps),
            intensity = IntensityFormatState.of(avgIntensity)
        )
    }

    /**
     * Calculate metrics for a list of exercises.
     */
    public fun calculateExercises(exercises: List<ExerciseState>): TrainingMetrics {
        val allIterations = exercises.flatMap { it.iterations }
        return calculateIterations(allIterations)
    }

    /**
     * Calculate metrics for multiple training sessions (each with its exercises).
     */
    public fun calculateTrainings(trainings: List<TrainingState>): TrainingMetrics {
        val allExercises = trainings.flatMap { it.exercises }
        return calculateExercises(allExercises)
    }
}