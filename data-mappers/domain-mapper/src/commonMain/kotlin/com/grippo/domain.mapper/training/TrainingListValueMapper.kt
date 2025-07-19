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

        val trainingPosition = when {
            nonEmptyTrainings.size == 1 -> TrainingPosition.SINGLE
            index == 0 -> TrainingPosition.FIRST
            index == nonEmptyTrainings.lastIndex -> TrainingPosition.LAST
            else -> TrainingPosition.MIDDLE
        }

        result += TrainingListValue.DateTime(
            date = training.createdAt,
            position = trainingPosition,
            id = "date-${training.id}"
        )

        when (exercises.size) {
            1 -> {
                val ex = exercises[0]
                result += TrainingListValue.SingleExercise(
                    exerciseState = ex,
                    position = trainingPosition,
                    id = "single-${training.id}-${ex.id}"
                )
            }

            2 -> {
                val first = exercises[0]
                val last = exercises[1]
                result += TrainingListValue.FirstExercise(
                    exerciseState = first,
                    position = trainingPosition,
                    id = "first-${training.id}-${first.id}"
                )
                result += TrainingListValue.BetweenExercises(
                    position = trainingPosition,
                    id = "between-${training.id}-${first.id}-${last.id}"
                )
                result += TrainingListValue.LastExercise(
                    exerciseState = last,
                    position = trainingPosition,
                    id = "last-${training.id}-${last.id}"
                )
            }

            else -> {
                val first = exercises.first()
                val last = exercises.last()
                val middle = exercises.subList(1, exercises.lastIndex)

                result += TrainingListValue.FirstExercise(
                    exerciseState = first,
                    position = trainingPosition,
                    id = "first-${training.id}-${first.id}"
                )
                result += TrainingListValue.BetweenExercises(
                    position = trainingPosition,
                    id = "between-${training.id}-${first.id}-${middle.first().id}"
                )
                middle.forEachIndexed { i, ex ->
                    result += TrainingListValue.MiddleExercise(
                        exerciseState = ex,
                        position = trainingPosition,
                        id = "middle-${training.id}-${ex.id}"
                    )

                    val next = middle.getOrNull(i + 1)
                        ?: last

                    result += TrainingListValue.BetweenExercises(
                        position = trainingPosition,
                        id = "between-${training.id}-${ex.id}-${next.id}"
                    )
                }

                result += TrainingListValue.LastExercise(
                    exerciseState = last,
                    position = trainingPosition,
                    id = "last-${training.id}-${last.id}"
                )
            }
        }

        result += TrainingListValue.TrainingSummary(
            training = training,
            position = trainingPosition,
            id = "summary-${training.id}"
        )
    }

    return result.toPersistentList()
}