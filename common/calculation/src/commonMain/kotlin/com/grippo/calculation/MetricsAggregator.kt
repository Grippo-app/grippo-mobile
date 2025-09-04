package com.grippo.calculation

import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.IterationState
import com.grippo.state.trainings.TrainingMetrics
import com.grippo.state.trainings.TrainingState

/**
 * 📊 **Metrics Aggregator**
 *
 * Calculates training metrics (Volume, Repetitions, Intensity) on 3 levels:
 *
 * - 🟢 **Iterations** → raw sets
 * - 🔵 **Exercises** → aggregate over multiple iterations
 * - 🟣 **Trainings** → aggregate over multiple exercises across sessions
 *
 * ---
 * 🔎 **Definitions**
 * - **Volume (kg·rep)**: Σ(weight × reps)
 * - **Repetitions**: Σ(reps)
 * - **Intensity (kg/rep)**: Volume ÷ Repetitions
 *
 * ⚠️ Valid only if: `weight != null` AND `reps != null` AND `reps > 0`
 */
public class MetricsAggregator {

    /** Aggregate metrics from raw iterations. */
    public fun calculateIterations(iterations: List<IterationState>): TrainingMetrics {
        return aggregateMetrics(iterations.asSequence())
    }

    /** Aggregate metrics from a list of exercises. */
    public fun calculateExercises(exercises: List<ExerciseState>): TrainingMetrics {
        val allIterations = exercises.asSequence().flatMap { it.iterations.asSequence() }
        return aggregateMetrics(allIterations)
    }

    /** Aggregate metrics from multiple training sessions. */
    public fun calculateTrainings(trainings: List<TrainingState>): TrainingMetrics {
        val allExercises = trainings.asSequence().flatMap { it.exercises.asSequence() }
        val allIterations = allExercises.flatMap { it.iterations.asSequence() }
        return aggregateMetrics(allIterations)
    }

    // ---- Core aggregation logic ----
    private fun aggregateMetrics(iterations: Sequence<IterationState>): TrainingMetrics {
        var sumTonnage = 0.0
        var sumReps = 0

        for (itn in iterations) {
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
}