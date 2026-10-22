package com.grippo.data.features.api.metrics.volume

import com.grippo.data.features.api.metrics.volume.models.TrainingTotal
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
                extraWeight = iteration.extraWeight,
                externalWeight = iteration.externalWeight,
                assistWeight = iteration.assistWeight,
                bodyWeight = iteration.bodyWeight,
                repetitions = iteration.repetitions,
                bodyMultiplier = iteration.bodyMultiplier
            )
        }
    }

    private fun aggregate(iterations: List<SetIteration>): TrainingTotal {
        if (iterations.isEmpty()) return emptyMetrics()

        var tonnage = 0.0
        var totalRepetitions = 0L
        var measuredRepetitions = 0L

        iterations.forEach { iteration ->
            val reps = iteration.repetitions
            if (reps <= 0) return@forEach

            totalRepetitions += reps.toLong()

            val weight = measuredWeight(iteration) ?: return@forEach
            tonnage += weight.toDouble() * reps.toDouble()
            // Fully assisted sets remain measured repetitions, so more assistance cannot raise the average.
            measuredRepetitions += reps.toLong()
        }

        val safeVolume = tonnage.coerceAtLeast(0.0).toFloat()
        val safeRepetitions = totalRepetitions
            .coerceIn(0, Int.MAX_VALUE.toLong())
            .toInt()
        val intensity = if (measuredRepetitions > 0L) {
            (tonnage / measuredRepetitions.toDouble()).toFloat()
        } else {
            0f
        }

        return TrainingTotal(
            volume = safeVolume,
            repetitions = safeRepetitions,
            intensity = intensity.coerceAtLeast(0f),
        )
    }

    private fun measuredWeight(iteration: SetIteration): Float? {
        val components = listOf(
            iteration.externalWeight,
            iteration.extraWeight,
            iteration.assistWeight,
            iteration.bodyWeight,
            iteration.bodyMultiplier,
        )
        if (components.any { it != null && (!it.isFinite() || it < 0f) }) {
            return null
        }

        val externalWeight = iteration.externalWeight
        if (externalWeight != null) {
            if (iteration.extraWeight != null || iteration.assistWeight != null ||
                iteration.bodyWeight != null || iteration.bodyMultiplier != null
            ) {
                return null
            }
            return externalWeight
        }

        val bodyWeight = iteration.bodyWeight ?: return null
        val bodyMultiplier = iteration.bodyMultiplier ?: return null
        if (bodyWeight <= 0f || bodyMultiplier <= 0f) {
            return null
        }
        if (iteration.extraWeight != null && iteration.assistWeight != null) {
            return null
        }

        return iteration.volume.takeIf { it.isFinite() }?.coerceAtLeast(0f)
    }

    private fun emptyMetrics(): TrainingTotal {
        return TrainingTotal(volume = 0f, repetitions = 0, intensity = 0f)
    }
}
