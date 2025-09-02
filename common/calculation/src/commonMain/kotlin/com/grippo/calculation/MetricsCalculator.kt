package com.grippo.calculation

import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingMetrics

public class MetricsCalculator {

    /**
     * 📦 Training Metrics (Volume, Repetitions, Intensity)
     *
     * WHAT:
     * - volume: total workload across all sets (expected: load * reps, in consistent units, e.g., kg·rep)
     * - repetitions: total repetitions across all sets
     * - intensity: average load per rep computed ONLY over valid sets
     *     valid set condition: volume != null AND reps != null AND reps > 0
     *     formula: avgIntensity = (Σ volume_valid_sets) / (Σ reps_valid_sets)
     *
     * WHY:
     * - Using a weighted mean for intensity avoids bias where tiny and huge exercises would contribute equally.
     *
     * PITFALLS:
     * - If your "volume" semantics differ from load*reps, rename metrics accordingly.
     * - Bodyweight/bands may not have meaningful "kg" volume without explicit load modeling.
     * - Negative or zero reps are ignored for intensity; totals still include zeroes.
     */
    public fun calculateTotalMetrics(exercises: List<ExerciseState>): TrainingMetrics {
        var totalVolumeD = 0.0       // accumulate as Double for precision, cast to Float at the end
        var totalReps = 0

        var sumVolumeValid = 0.0     // for intensity numerator (only valid sets)
        var sumRepsValid = 0         // for intensity denominator (only valid sets)

        // Single pass over all sets to avoid inconsistencies
        exercises.forEach { ex ->
            ex.iterations.forEach { itn ->
                // Totals (lenient): treat null as zero to keep backward-compat totals
                totalVolumeD += (itn.volume.value ?: 0f).toDouble()
                totalReps += (itn.repetitions.value ?: 0)

                // Intensity (strict): include only if both volume and reps are valid and reps > 0
                val v = itn.volume.value
                val r = itn.repetitions.value
                if (v != null && r != null && r > 0) {
                    sumVolumeValid += v.toDouble()
                    sumRepsValid += r
                }
            }
        }

        val totalVolume = totalVolumeD.toFloat().coerceAtLeast(0f)
        val averageIntensity = if (sumRepsValid > 0) {
            (sumVolumeValid / sumRepsValid).toFloat().coerceAtLeast(0f)
        } else 0f

        return TrainingMetrics(
            volume = VolumeFormatState.of(totalVolume),
            repetitions = RepetitionsFormatState.of(totalReps),
            intensity = IntensityFormatState.of(averageIntensity)
        )
    }
}