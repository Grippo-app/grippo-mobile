package com.grippo.calculation.internal.training

import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.IterationState
import com.grippo.core.state.trainings.TrainingMetrics
import com.grippo.core.state.trainings.TrainingState

/**
 * Metrics aggregator for volume, repetitions, and intensity across sets, exercises, and sessions.
 */
internal class MetricsAggregator {

    /** Aggregate metrics from raw iterations. */
    fun calculateIterations(iterations: List<IterationState>): TrainingMetrics {
        var sumTonnage = 0.0
        var repsAll: Long = 0
        var repsWithWeight: Long = 0

        iterations.forEach { iteration ->
            accumulate(
                iteration,
                sumT = { sum -> sumTonnage += sum },
                incAll = { delta -> repsAll += delta },
                incWithW = { delta -> repsWithWeight += delta },
            )
        }
        return buildMetrics(sumTonnage, repsAll, repsWithWeight)
    }

    /** Aggregate metrics from a list of exercises. */
    fun calculateExercises(exercises: List<ExerciseState>): TrainingMetrics {
        var sumTonnage = 0.0
        var repsAll: Long = 0
        var repsWithWeight: Long = 0

        exercises.forEach { exercise ->
            exercise.iterations.forEach { iteration ->
                accumulate(
                    iteration,
                    sumT = { sum -> sumTonnage += sum },
                    incAll = { delta -> repsAll += delta },
                    incWithW = { delta -> repsWithWeight += delta },
                )
            }
        }
        return buildMetrics(sumTonnage, repsAll, repsWithWeight)
    }

    /** Aggregate metrics from multiple training sessions. */
    fun calculateTrainings(trainings: List<TrainingState>): TrainingMetrics {
        var sumTonnage = 0.0
        var repsAll: Long = 0
        var repsWithWeight: Long = 0

        trainings.forEach { training ->
            training.exercises.forEach { exercise ->
                exercise.iterations.forEach { iteration ->
                    accumulate(
                        iteration,
                        sumT = { sum -> sumTonnage += sum },
                        incAll = { delta -> repsAll += delta },
                        incWithW = { delta -> repsWithWeight += delta },
                    )
                }
            }
        }
        return buildMetrics(sumTonnage, repsAll, repsWithWeight)
    }

    private inline fun accumulate(
        iteration: IterationState,
        sumT: (Double) -> Unit,
        incAll: (Long) -> Unit,
        incWithW: (Long) -> Unit,
    ) {
        val weight = (iteration.volume as? VolumeFormatState.Valid)?.value
        val reps = (iteration.repetitions as? RepetitionsFormatState.Valid)?.value

        if (reps == null || reps <= 0) return
        incAll(reps.toLong())

        if (weight != null && weight > 0f) {
            sumT(weight.toDouble() * reps.toDouble())
            incWithW(reps.toLong())
        }
    }

    private fun buildMetrics(
        sumTonnage: Double,
        repsAll: Long,
        repsWithWeight: Long,
    ): TrainingMetrics {
        val safeVolume = sumTonnage.coerceAtLeast(0.0).toFloat()

        val safeRepsAll = repsAll.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        val safeRepsWithWeight = repsWithWeight.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()

        val avgIntensity = if (safeRepsWithWeight > 0) {
            (sumTonnage / safeRepsWithWeight.toDouble()).toFloat()
        } else {
            0f
        }

        return TrainingMetrics(
            volume = VolumeFormatState.of(safeVolume),
            repetitions = RepetitionsFormatState.of(safeRepsAll),
            intensity = IntensityFormatState.of(avgIntensity),
        )
    }
}
