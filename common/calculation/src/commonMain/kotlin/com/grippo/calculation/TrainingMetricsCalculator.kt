package com.grippo.calculation

import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingMetrics

public class TrainingMetricsCalculator {

    /**
     * Session totals from raw iterations (assumes iteration.volume stores load per rep, not tonnage).
     *
     * Valid set: weight != null AND reps != null AND reps > 0
     * Volume (tonnage)   = Σ(weight * reps) over valid sets
     * Repetitions        = Σ(reps)          over valid sets
     * Intensity (kg/rep) = Volume / Repetitions
     */
    public fun calculateTotalMetrics(exercises: List<ExerciseState>): TrainingMetrics {
        var sumTonnage = 0.0   // Σ(weight * reps) over valid sets
        var sumReps = 0        // Σ(reps)          over valid sets

        exercises.forEach { ex ->
            ex.iterations.forEach { itn ->
                val weight = (itn.volume as? VolumeFormatState.Valid)?.value
                val reps = (itn.repetitions as? RepetitionsFormatState.Valid)?.value
                if (weight != null && reps != null && reps > 0) {
                    sumTonnage += weight.toDouble() * reps.toDouble()
                    sumReps += reps
                }
            }
        }

        val totalVolume = sumTonnage.toFloat().coerceAtLeast(0f) // units: kg·rep
        val totalReps = sumReps.coerceAtLeast(0)
        val avgIntensity = if (totalReps > 0) totalVolume / totalReps else 0f

        return TrainingMetrics(
            volume = VolumeFormatState.of(totalVolume),          // kg·rep
            repetitions = RepetitionsFormatState.of(totalReps),  // reps
            intensity = IntensityFormatState.of(avgIntensity)    // kg/rep
        )
    }
}