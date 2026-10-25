package com.grippo.data.features.api.metrics.performance

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleComponents
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.api.metrics.performance.models.EstimatedOneRepMaxEntry
import com.grippo.data.features.api.metrics.performance.models.EstimatedOneRepMaxLoadType
import com.grippo.data.features.api.metrics.performance.models.EstimatedOneRepMaxSeries
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.Training

public class EstimatedOneRepMaxUseCase {

    public fun fromExercises(
        exercises: List<Exercise>,
        exerciseExample: ExerciseExample,
    ): EstimatedOneRepMaxSeries {
        val entries = exercises
            .filter { it.exerciseExample.id == exerciseExample.value.id }
            .sortedWith(compareBy<Exercise> { it.createdAt }.thenBy { it.id })
            .mapNotNull { exercise ->
                val best = exercise.iterations
                    .mapNotNull { iteration ->
                        estimateSet(
                            iteration = iteration,
                            weightType = exercise.exerciseExample.weightType,
                            components = exerciseExample.components,
                        )
                    }
                    .maxByOrNull { it.value }
                    ?: return@mapNotNull null

                EstimatedOneRepMaxEntry(
                    start = exercise.createdAt,
                    value = best.value,
                    weight = best.weight,
                    repetitions = best.repetitions,
                    loadType = best.loadType,
                )
            }

        return EstimatedOneRepMaxSeries(entries = entries)
    }

    public fun fromTrainings(
        trainings: List<Training>,
        exerciseExample: ExerciseExample,
    ): EstimatedOneRepMaxSeries {
        val exercises = trainings.flatMap(Training::exercises)
        return fromExercises(exercises = exercises, exerciseExample = exerciseExample)
    }

    private fun estimateSet(
        iteration: Iteration,
        weightType: WeightTypeEnum,
        components: ExerciseExampleComponents,
    ): SetEstimate? {
        if (iteration.repetitions !in 1..10) {
            return null
        }
        if (!isValidOptionalLoad(iteration.externalWeight) ||
            !isValidOptionalLoad(iteration.extraWeight) ||
            !isValidOptionalLoad(iteration.assistWeight) ||
            !isValidOptionalLoad(iteration.bodyWeight) ||
            !isValidOptionalLoad(iteration.bodyMultiplier)
        ) {
            return null
        }

        val loadType: EstimatedOneRepMaxLoadType
        val weight: Double
        when (weightType) {
            WeightTypeEnum.FREE, WeightTypeEnum.FIXED -> {
                if (components !is ExerciseExampleComponents.External ||
                    iteration.bodyWeight != null || iteration.bodyMultiplier != null ||
                    iteration.extraWeight != null || iteration.assistWeight != null
                ) {
                    return null
                }
                val externalWeight = iteration.externalWeight ?: return null
                if (externalWeight <= 0f) {
                    return null
                }
                loadType = EstimatedOneRepMaxLoadType.External
                weight = externalWeight.toDouble()
            }

            WeightTypeEnum.BODY_WEIGHT -> {
                if (iteration.externalWeight != null) {
                    return null
                }
                val completeLoad = when (components) {
                    is ExerciseExampleComponents.External -> false
                    is ExerciseExampleComponents.BodyOnly ->
                        iteration.extraWeight == null && iteration.assistWeight == null

                    is ExerciseExampleComponents.BodyAndExtra ->
                        iteration.assistWeight == null &&
                            (!components.extraRequired || iteration.extraWeight != null)

                    is ExerciseExampleComponents.BodyAndAssist ->
                        iteration.extraWeight == null &&
                            (!components.assistRequired || iteration.assistWeight != null)
                }
                if (!completeLoad) {
                    return null
                }
                val bodyWeight = iteration.bodyWeight ?: return null
                val bodyMultiplier = iteration.bodyMultiplier ?: return null
                if (bodyWeight <= 0f || bodyMultiplier <= 0f) {
                    return null
                }
                // Match the recorded Float product so fully offset assistance remains exactly zero.
                val bodyLoad = bodyWeight * bodyMultiplier
                loadType = EstimatedOneRepMaxLoadType.Effective
                weight = bodyLoad.toDouble() +
                    (iteration.extraWeight?.toDouble() ?: 0.0) -
                    (iteration.assistWeight?.toDouble() ?: 0.0)
            }
        }
        if (!weight.isFinite() || weight <= 0.0) {
            return null
        }

        val value = if (iteration.repetitions == 1) {
            weight
        } else {
            val repetitions = iteration.repetitions.toDouble()
            val epley = weight * (1.0 + repetitions / 30.0)
            val brzycki = weight * 36.0 / (37.0 - repetitions)
            (epley + brzycki) / 2.0
        }
        val sourceWeight = weight.toFloat()
        val estimate = value.toFloat()
        if (!sourceWeight.isFinite() || sourceWeight <= 0f || !estimate.isFinite() || estimate <= 0f) {
            return null
        }

        return SetEstimate(
            value = estimate,
            weight = sourceWeight,
            repetitions = iteration.repetitions,
            loadType = loadType,
        )
    }

    private fun isValidOptionalLoad(value: Float?): Boolean {
        return value == null || (value.isFinite() && value >= 0f)
    }

    private data class SetEstimate(
        val value: Float,
        val weight: Float,
        val repetitions: Int,
        val loadType: EstimatedOneRepMaxLoadType,
    )
}
