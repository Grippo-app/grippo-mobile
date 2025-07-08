package com.grippo.domain.mapper.training

import com.grippo.presentation.api.trainings.models.TrainingListValue
import com.grippo.presentation.api.trainings.models.TrainingPosition
import com.grippo.presentation.api.trainings.models.TrainingState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<TrainingState>.transformToTrainingListValue(): ImmutableList<TrainingListValue> {
    val result = mutableListOf<TrainingListValue>()

    val nonEmptyTrainings = this.filter { it.exercises.isNotEmpty() }

    nonEmptyTrainings.forEachIndexed { index, training ->
        val exercises = training.exercises
        if (exercises.isEmpty()) return@forEachIndexed

        val trainingPosition = when {
            nonEmptyTrainings.size == 1 -> TrainingPosition.SINGLE
            index == 0 -> TrainingPosition.FIRST
            index == nonEmptyTrainings.lastIndex -> TrainingPosition.LAST
            else -> TrainingPosition.MIDDLE
        }

        result += TrainingListValue.DateTime(
            date = training.createdAt,
            position = trainingPosition
        )

        when (exercises.size) {
            1 -> result += TrainingListValue.SingleExercise(
                exerciseState = exercises[0],
                position = trainingPosition
            )

            2 -> {
                result += TrainingListValue.FirstExercise(
                    exerciseState = exercises[0],
                    position = trainingPosition
                )
                result += TrainingListValue.LastExercise(
                    exerciseState = exercises[1],
                    position = trainingPosition
                )
            }

            else -> {
                result += TrainingListValue.FirstExercise(
                    exerciseState = exercises.first(),
                    position = trainingPosition
                )
                exercises.subList(1, exercises.lastIndex).forEach {
                    result += TrainingListValue.MiddleExercise(
                        exerciseState = it,
                        position = trainingPosition
                    )
                }
                result += TrainingListValue.LastExercise(
                    exerciseState = exercises.last(),
                    position = trainingPosition
                )
            }
        }
    }

    return result.toPersistentList()
}