package com.grippo.data.features.api.metrics.distribution

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.api.metrics.distribution.models.DistributionWeighting
import com.grippo.data.features.api.metrics.distribution.models.ExerciseDistribution
import com.grippo.data.features.api.metrics.distribution.models.ExerciseDistributionEntry
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.Training

/** Groups recorded exercise work by catalog classification; only positive repetitions contribute. */
public class ExerciseDistributionUseCase {

    public fun categoriesFromTrainings(
        trainings: List<Training>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): ExerciseDistribution<CategoryEnum> {
        val exercises = trainings.flatMap(Training::exercises)
        return categoriesFromExercises(exercises = exercises, weighting = weighting)
    }

    public fun categoriesFromExercises(
        exercises: List<Exercise>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): ExerciseDistribution<CategoryEnum> {
        return buildDistribution(
            exercises = exercises,
            weighting = weighting,
            ordered = CategoryEnum.entries,
            keySelector = { it.exerciseExample.category }
        )
    }

    public fun weightTypesFromTrainings(
        trainings: List<Training>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): ExerciseDistribution<WeightTypeEnum> {
        val exercises = trainings.flatMap(Training::exercises)
        return weightTypesFromExercises(exercises = exercises, weighting = weighting)
    }

    public fun weightTypesFromExercises(
        exercises: List<Exercise>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): ExerciseDistribution<WeightTypeEnum> {
        return buildDistribution(
            exercises = exercises,
            weighting = weighting,
            ordered = WeightTypeEnum.entries,
            keySelector = { it.exerciseExample.weightType }
        )
    }

    public fun forceTypesFromTrainings(
        trainings: List<Training>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): ExerciseDistribution<ForceTypeEnum> {
        val exercises = trainings.flatMap(Training::exercises)
        return forceTypesFromExercises(exercises = exercises, weighting = weighting)
    }

    public fun forceTypesFromExercises(
        exercises: List<Exercise>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): ExerciseDistribution<ForceTypeEnum> {
        return buildDistribution(
            exercises = exercises,
            weighting = weighting,
            ordered = ForceTypeEnum.entries,
            keySelector = { it.exerciseExample.forceType }
        )
    }

    private fun <K : Enum<K>> buildDistribution(
        exercises: List<Exercise>,
        weighting: DistributionWeighting,
        ordered: List<K>,
        keySelector: (Exercise) -> K,
    ): ExerciseDistribution<K> {
        val totals = HashMap<K, Double>()
        var excludedVolumeSetCount = 0L

        for (exercise in exercises) {
            val contribution = when (weighting) {
                DistributionWeighting.Count -> {
                    if (exercise.iterations.any { it.repetitions > 0 }) {
                        1.0
                    } else {
                        0.0
                    }
                }

                DistributionWeighting.Sets -> {
                    exercise.iterations.count { it.repetitions > 0 }.toDouble()
                }

                DistributionWeighting.Repetitions -> {
                    exercise.iterations.sumOf { it.repetitions.coerceAtLeast(0).toDouble() }
                }

                DistributionWeighting.Volume -> {
                    var volume = 0.0
                    for (iteration in exercise.iterations) {
                        if (iteration.repetitions <= 0) {
                            continue
                        }
                        val load = effectiveLoad(iteration)
                        if (load == null) {
                            excludedVolumeSetCount += 1L
                            continue
                        }
                        volume += load * iteration.repetitions.toDouble()
                    }
                    volume
                }
            }

            if (contribution > 0.0) {
                val key = keySelector(exercise)
                totals[key] = (totals[key] ?: 0.0) + contribution
            }
        }

        val entries = ordered.mapNotNull { key ->
            val value = totals[key] ?: return@mapNotNull null
            ExerciseDistributionEntry(
                key = key,
                value = value
            )
        }
        return ExerciseDistribution(
            weighting = weighting,
            entries = entries,
            excludedVolumeSetCount = excludedVolumeSetCount
        )
    }

    private fun effectiveLoad(iteration: Iteration): Double? {
        val components = listOf(
            iteration.externalWeight,
            iteration.extraWeight,
            iteration.assistWeight,
            iteration.bodyWeight,
            iteration.bodyMultiplier
        )
        if (components.any { it != null && (!it.isFinite() || it < 0f) }) {
            return null
        }

        val body = iteration.bodyWeight
        val multiplier = iteration.bodyMultiplier
        val bodyLoad = if (body != null || multiplier != null) {
            if (body == null || multiplier == null || body <= 0f || multiplier <= 0f) {
                return null
            }
            val recordedBodyLoad = body * multiplier
            if (!recordedBodyLoad.isFinite()) {
                return null
            }
            recordedBodyLoad.toDouble()
        } else {
            0.0
        }

        // Preserve the recorded body product; accumulate distinct load terms without Float overflow.
        val load = (iteration.externalWeight?.toDouble() ?: 0.0) +
            (iteration.extraWeight?.toDouble() ?: 0.0) + bodyLoad -
            (iteration.assistWeight?.toDouble() ?: 0.0)
        return load.takeIf { it > 0.0 }
    }
}
