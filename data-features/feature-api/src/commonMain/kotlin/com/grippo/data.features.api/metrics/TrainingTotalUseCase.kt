package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.metrics.models.TrainingTotal
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.data.features.api.training.models.Training

public class TrainingTotalUseCase {

    public fun fromSetIterations(iterations: List<SetIteration>): TrainingTotal {
        return aggregate(iterations)
    }

    public fun fromSetExercises(exercises: List<SetExercise>): TrainingTotal {
        if (exercises.isEmpty()) return emptyMetrics()
        val iterations = exercises.flatMap(SetExercise::iterations)
        return aggregate(iterations)
    }

    public fun fromIterations(iterations: List<Iteration>): TrainingTotal {
        if (iterations.isEmpty()) return emptyMetrics()
        return aggregate(iterations.toSetIterations())
    }

    public fun fromExercises(exercises: List<Exercise>): TrainingTotal {
        if (exercises.isEmpty()) return emptyMetrics()
        val iterations = exercises.flatMap(Exercise::iterations)
        return fromIterations(iterations)
    }

    public fun fromTrainings(trainings: List<Training>): TrainingTotal {
        if (trainings.isEmpty()) return emptyMetrics()
        val iterations = trainings.flatMap { training ->
            training.exercises.flatMap(Exercise::iterations)
        }
        return fromIterations(iterations)
    }

    private fun List<Iteration>.toSetIterations(): List<SetIteration> {
        return map { iteration ->
            SetIteration(
                volume = iteration.volume,
                repetitions = iteration.repetitions,
            )
        }
    }

    private fun aggregate(iterations: List<SetIteration>): TrainingTotal {
        if (iterations.isEmpty()) return emptyMetrics()

        var tonnage = 0.0
        var totalRepetitions = 0L
        var repetitionsWithWeight = 0L

        iterations.forEach { iteration ->
            val reps = iteration.repetitions
            if (reps <= 0) return@forEach

            totalRepetitions += reps.toLong()

            val weight = iteration.volume
            if (weight > 0f) {
                tonnage += weight.toDouble() * reps.toDouble()
                repetitionsWithWeight += reps.toLong()
            }
        }

        val safeVolume = tonnage.coerceAtLeast(0.0).toFloat()
        val safeRepetitions = totalRepetitions
            .coerceIn(0, Int.MAX_VALUE.toLong())
            .toInt()
        val safeRepsWithWeight = repetitionsWithWeight
            .coerceIn(0, Int.MAX_VALUE.toLong())
            .toInt()

        val intensity = if (safeRepsWithWeight > 0) {
            (tonnage / safeRepsWithWeight.toDouble()).toFloat()
        } else {
            0f
        }

        return TrainingTotal(
            volume = safeVolume,
            repetitions = safeRepetitions,
            intensity = intensity.coerceAtLeast(0f),
        )
    }

    private fun emptyMetrics(): TrainingTotal {
        return TrainingTotal(volume = 0f, repetitions = 0, intensity = 0f)
    }
}
