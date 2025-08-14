package com.grippo.domain.state.training

import com.grippo.state.trainings.TrainingListValue
import com.grippo.state.trainings.TrainingPosition
import com.grippo.state.trainings.TrainingState
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
            id = "date-${training.id}",
            volume = training.volume,
            repetitions = training.repetitions
        )

        when (exercises.size) {
            1 -> {
                val ex = exercises[0]
                result += TrainingListValue.SingleExercise(
                    exerciseState = ex,
                    position = trainingPosition,
                    id = "single-${training.id}-${ex.id}",
                    indexInTraining = 1
                )
            }

            2 -> {
                val first = exercises[0]
                val last = exercises[1]
                result += TrainingListValue.FirstExercise(
                    exerciseState = first,
                    position = trainingPosition,
                    id = "first-${training.id}-${first.id}",
                    indexInTraining = 1
                )
                result += TrainingListValue.BetweenExercises(
                    position = trainingPosition,
                    id = "between-${training.id}-${first.id}-${last.id}"
                )
                result += TrainingListValue.LastExercise(
                    exerciseState = last,
                    position = trainingPosition,
                    id = "last-${training.id}-${last.id}",
                    indexInTraining = 2
                )
            }

            else -> {
                val first = exercises.first()
                val last = exercises.last()
                val middle = exercises.subList(1, exercises.lastIndex)

                result += TrainingListValue.FirstExercise(
                    exerciseState = first,
                    position = trainingPosition,
                    id = "first-${training.id}-${first.id}",
                    indexInTraining = 1
                )

                result += TrainingListValue.BetweenExercises(
                    position = trainingPosition,
                    id = "between-${training.id}-${first.id}-${middle.first().id}"
                )

                middle.forEachIndexed { i, ex ->
                    val indexInTraining = i + 2 // +2 because 1 is First, 2+ is Middle

                    result += TrainingListValue.MiddleExercise(
                        exerciseState = ex,
                        position = trainingPosition,
                        id = "middle-${training.id}-${ex.id}",
                        indexInTraining = indexInTraining
                    )

                    val next = middle.getOrNull(i + 1) ?: last

                    result += TrainingListValue.BetweenExercises(
                        position = trainingPosition,
                        id = "between-${training.id}-${ex.id}-${next.id}"
                    )
                }

                result += TrainingListValue.LastExercise(
                    exerciseState = last,
                    position = trainingPosition,
                    id = "last-${training.id}-${last.id}",
                    indexInTraining = exercises.size
                )
            }
        }
    }

    return result.toPersistentList()
}