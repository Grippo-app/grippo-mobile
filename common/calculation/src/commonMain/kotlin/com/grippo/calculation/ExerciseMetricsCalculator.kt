package com.grippo.calculation

import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.IterationState
import com.grippo.state.trainings.TrainingMetrics

/**
 * Computes per-exercise metrics from its iterations.
 *
 * Rules:
 * - "Volume" here means tonnage = Σ(load * reps) across valid sets.
 * - A "valid set" is a set with both load (volume.value) != null and reps > 0.
 * - Intensity = (Σ load*reps) / (Σ reps) over valid sets only.
 * - All accumulations are done in Double for numeric stability, then cast to Float.
 */
public class ExerciseMetricsCalculator {

    public fun compute(iterations: List<IterationState>): TrainingMetrics {
        var sumTonnage = 0.0   // Σ(load * reps) over valid sets
        var sumReps = 0        // Σ(reps) over valid sets

        iterations.forEach { itn ->
            val load = (itn.volume as? VolumeFormatState.Valid)?.value
            val reps = (itn.repetitions as? RepetitionsFormatState.Valid)?.value
            if (load != null && reps != null && reps > 0) {
                sumTonnage += load.toDouble() * reps.toDouble()
                sumReps += reps
            }
        }

        val totalVolume = sumTonnage.toFloat().coerceAtLeast(0f)
        val totalReps = sumReps.coerceAtLeast(0)
        val avgIntensity = if (totalReps > 0) (totalVolume / totalReps) else 0f

        return TrainingMetrics(
            volume = VolumeFormatState.of(totalVolume),
            repetitions = RepetitionsFormatState.of(totalReps),
            intensity = IntensityFormatState.of(avgIntensity)
        )
    }

    /** Convenience helper if you want to pass an ExerciseState instead of iterations. */
    public fun compute(exercise: ExerciseState): TrainingMetrics =
        compute(exercise.iterations)
}