package com.grippo.calculation

import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.IterationState
import com.grippo.state.trainings.TrainingMetrics
import com.grippo.state.trainings.TrainingState

/**
 * 📊 Metrics Aggregator
 *
 * Calculates Volume, Repetitions, and Intensity at three levels:
 * - Iterations → raw sets
 * - Exercises  → aggregate over iterations
 * - Trainings  → aggregate over exercises across sessions
 *
 * Definitions
 * - Volume (kg·rep): Σ(weight × reps) using only sets with positive weight and reps > 0
 * - Repetitions: Σ(reps) across all valid sets (weight may be missing/zero)
 * - Intensity (kg/rep): Volume ÷ Repetitions (only where weight is valid) ⇒ reps-weighted average load
 */
public class MetricsAggregator {

    /** Aggregate metrics from raw iterations. */
    public fun calculateIterations(iterations: List<IterationState>): TrainingMetrics {
        var sumTonnage = 0.0
        var repsAll: Long = 0
        var repsWithWeight: Long = 0

        for (itn in iterations) {
            accumulate(
                itn,
                sumT = { sum -> sumTonnage += sum },
                incAll = { d -> repsAll += d },
                incWithW = { d -> repsWithWeight += d }
            )
        }
        return buildMetrics(sumTonnage, repsAll, repsWithWeight)
    }

    /** Aggregate metrics from a list of exercises. */
    public fun calculateExercises(exercises: List<ExerciseState>): TrainingMetrics {
        var sumTonnage = 0.0
        var repsAll: Long = 0
        var repsWithWeight: Long = 0

        for (ex in exercises) {
            for (itn in ex.iterations) {
                accumulate(
                    itn,
                    sumT = { sum -> sumTonnage += sum },
                    incAll = { d -> repsAll += d },
                    incWithW = { d -> repsWithWeight += d }
                )
            }
        }
        return buildMetrics(sumTonnage, repsAll, repsWithWeight)
    }

    /** Aggregate metrics from multiple training sessions. */
    public fun calculateTrainings(trainings: List<TrainingState>): TrainingMetrics {
        var sumTonnage = 0.0
        var repsAll: Long = 0
        var repsWithWeight: Long = 0

        for (tr in trainings) {
            for (ex in tr.exercises) {
                for (itn in ex.iterations) {
                    accumulate(
                        itn,
                        sumT = { sum -> sumTonnage += sum },
                        incAll = { d -> repsAll += d },
                        incWithW = { d -> repsWithWeight += d }
                    )
                }
            }
        }
        return buildMetrics(sumTonnage, repsAll, repsWithWeight)
    }

    // ---- Core helpers (English-only comments) ----

    private inline fun accumulate(
        itn: IterationState,
        sumT: (Double) -> Unit,
        incAll: (Long) -> Unit,
        incWithW: (Long) -> Unit
    ) {
        val weight = (itn.volume as? VolumeFormatState.Valid)?.value
        val reps = (itn.repetitions as? RepetitionsFormatState.Valid)?.value

        // Guard invalid/negative reps
        if (reps == null || reps <= 0) return

        // Count all valid reps (even if weight is missing/zero)
        incAll(reps.toLong())

        // Only positive known weight contributes to Volume and Intensity
        if (weight != null && weight > 0f) {
            sumT(weight.toDouble() * reps.toDouble())
            incWithW(reps.toLong())
        }
    }

    private fun buildMetrics(
        sumTonnage: Double,
        repsAll: Long,
        repsWithWeight: Long
    ): TrainingMetrics {
        val safeVolume = sumTonnage.coerceAtLeast(0.0).toFloat()

        val safeRepsAll: Int = when {
            repsAll < 0 -> 0
            repsAll > Int.MAX_VALUE -> Int.MAX_VALUE
            else -> repsAll.toInt()
        }

        val safeRepsWithW: Int = when {
            repsWithWeight <= 0 -> 0
            repsWithWeight > Int.MAX_VALUE -> Int.MAX_VALUE
            else -> repsWithWeight.toInt()
        }

        val avgIntensity: Float = if (safeRepsWithW > 0) {
            (sumTonnage / safeRepsWithW.toDouble()).toFloat()
        } else {
            0f
        }

        return TrainingMetrics(
            volume = VolumeFormatState.of(safeVolume),
            repetitions = RepetitionsFormatState.of(safeRepsAll),
            intensity = IntensityFormatState.of(avgIntensity)
        )
    }
}