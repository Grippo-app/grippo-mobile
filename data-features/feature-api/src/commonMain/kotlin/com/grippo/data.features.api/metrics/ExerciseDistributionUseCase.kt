package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.api.metrics.models.DistributionWeighting
import com.grippo.data.features.api.metrics.models.ExerciseDistribution
import com.grippo.data.features.api.metrics.models.ExerciseDistributionEntry
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.Training

public class ExerciseDistributionUseCase {

    private companion object {
        private const val EPS = 1e-3f
    }

    public fun categoriesFromTrainings(
        trainings: List<Training>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): ExerciseDistribution<CategoryEnum> {
        val exercises = trainings.flatMap(Training::exercises)
        return categoriesFromExercises(exercises, weighting)
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
        return weightTypesFromExercises(exercises, weighting)
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
        return forceTypesFromExercises(exercises, weighting)
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
        keySelector: (Exercise) -> K?,
    ): ExerciseDistribution<K> {
        if (exercises.isEmpty()) return ExerciseDistribution(emptyList())
        val totals = aggregateTotals(exercises, weighting, keySelector)
        if (totals.isEmpty()) return ExerciseDistribution(emptyList())

        val entries = ordered.mapNotNull { key ->
            val value = totals[key]?.takeIf { it > EPS } ?: return@mapNotNull null
            ExerciseDistributionEntry(
                key = key,
                value = value
            )
        }
        return ExerciseDistribution(entries)
    }

    private fun <K : Enum<K>> aggregateTotals(
        exercises: List<Exercise>,
        weighting: DistributionWeighting,
        keySelector: (Exercise) -> K?,
    ): Map<K, Float> {
        val totals = HashMap<K, Float>(8)
        exercises.forEach { exercise ->
            val key = keySelector(exercise) ?: return@forEach
            val contribution = weightOfExercise(exercise, weighting)
            if (contribution <= EPS) return@forEach
            val currentValue = totals[key] ?: 0f
            totals[key] = currentValue + contribution
        }
        return totals
    }

    private fun weightOfExercise(
        exercise: Exercise,
        weighting: DistributionWeighting,
    ): Float = when (weighting) {
        DistributionWeighting.Count -> 1f
        DistributionWeighting.Sets -> exercise.iterations.count { it.repetitions > 0 }.toFloat()
        DistributionWeighting.Repetitions -> exercise.iterations.fold(0) { acc, iteration ->
            val reps = iteration.repetitions
            if (reps > 0) acc + reps else acc
        }.toFloat()

        DistributionWeighting.Volume -> exercise.iterations.computeVolume()
    }.coerceAtLeast(0f)

    private fun List<Iteration>.computeVolume(): Float {
        if (isEmpty()) return 0f
        val total = fold(0.0) { acc, iteration ->
            val reps = iteration.repetitions
            if (reps <= 0) return@fold acc
            val weight = iteration.volume
            if (weight <= EPS) acc else acc + weight.toDouble() * reps.toDouble()
        }
        return total.toFloat()
    }
}
