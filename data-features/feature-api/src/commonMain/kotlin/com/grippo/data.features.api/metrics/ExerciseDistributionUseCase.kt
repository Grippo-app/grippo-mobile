package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.api.metrics.models.DistributionWeighting
import com.grippo.data.features.api.metrics.models.ExerciseDistribution
import com.grippo.data.features.api.metrics.models.ExerciseDistributionEntry
import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.data.features.api.training.models.SetTraining

public class ExerciseDistributionUseCase {

    private companion object {
        private const val EPS = 1e-3f
    }

    public fun categoriesFromTrainings(
        trainings: List<SetTraining>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): ExerciseDistribution<CategoryEnum> {
        val exercises = trainings.flatMap(SetTraining::exercises)
        return categoriesFromExercises(exercises, weighting)
    }

    public fun categoriesFromExercises(
        exercises: List<SetExercise>,
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
        trainings: List<SetTraining>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): ExerciseDistribution<WeightTypeEnum> {
        val exercises = trainings.flatMap(SetTraining::exercises)
        return weightTypesFromExercises(exercises, weighting)
    }

    public fun weightTypesFromExercises(
        exercises: List<SetExercise>,
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
        trainings: List<SetTraining>,
        weighting: DistributionWeighting = DistributionWeighting.Count,
    ): ExerciseDistribution<ForceTypeEnum> {
        val exercises = trainings.flatMap(SetTraining::exercises)
        return forceTypesFromExercises(exercises, weighting)
    }

    public fun forceTypesFromExercises(
        exercises: List<SetExercise>,
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
        exercises: List<SetExercise>,
        weighting: DistributionWeighting,
        ordered: List<K>,
        keySelector: (SetExercise) -> K?,
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
        exercises: List<SetExercise>,
        weighting: DistributionWeighting,
        keySelector: (SetExercise) -> K?,
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
        exercise: SetExercise,
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

    private fun List<SetIteration>.computeVolume(): Float {
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
