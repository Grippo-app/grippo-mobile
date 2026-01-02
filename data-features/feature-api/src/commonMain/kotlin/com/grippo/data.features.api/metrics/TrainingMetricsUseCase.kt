package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.metrics.models.TrainingMetrics
import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.data.features.api.training.models.SetTraining

public class TrainingMetricsUseCase {

    public fun fromIterations(iterations: List<SetIteration>): TrainingMetrics {
        return aggregate(iterations)
    }

    public fun fromExercises(exercises: List<SetExercise>): TrainingMetrics {
        if (exercises.isEmpty()) return emptyMetrics()
        val iterations = exercises.flatMap(SetExercise::iterations)
        return aggregate(iterations)
    }

    public fun fromTrainings(trainings: List<SetTraining>): TrainingMetrics {
        if (trainings.isEmpty()) return emptyMetrics()
        val iterations = trainings.flatMap { training ->
            training.exercises.flatMap(SetExercise::iterations)
        }
        return aggregate(iterations)
    }

    private fun aggregate(iterations: List<SetIteration>): TrainingMetrics {
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

        return TrainingMetrics(
            volume = safeVolume,
            repetitions = safeRepetitions,
            intensity = intensity.coerceAtLeast(0f),
        )
    }

    private fun emptyMetrics(): TrainingMetrics {
        return TrainingMetrics(volume = 0f, repetitions = 0, intensity = 0f)
    }
}
