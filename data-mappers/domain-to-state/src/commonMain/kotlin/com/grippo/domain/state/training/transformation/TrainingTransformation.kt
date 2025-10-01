package com.grippo.domain.state.training.transformation

import com.grippo.state.trainings.TrainingListValue
import com.grippo.state.trainings.TrainingPosition
import com.grippo.state.trainings.TrainingState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

public fun TrainingState.toTrainingListValues(
    position: TrainingPosition = TrainingPosition.SINGLE,
): ImmutableList<TrainingListValue> {
    val exercises = this.exercises
    if (exercises.isEmpty()) return persistentListOf()

    val result = mutableListOf<TrainingListValue>()

    when (exercises.size) {
        1 -> {
            val ex = exercises[0]
            result += TrainingListValue.SingleExercise(
                exerciseState = ex,
                position = position,
                key = "single-${this.id}-${ex.id}",
                indexInTraining = 1
            )
        }

        2 -> {
            val first = exercises[0]
            val last = exercises[1]

            result += TrainingListValue.FirstExercise(
                exerciseState = first,
                position = position,
                key = "first-${this.id}-${first.id}",
                indexInTraining = 1
            )
            result += TrainingListValue.BetweenExercises(
                position = position,
                key = "between-${this.id}-${first.id}-${last.id}"
            )
            result += TrainingListValue.LastExercise(
                exerciseState = last,
                position = position,
                key = "last-${this.id}-${last.id}",
                indexInTraining = 2
            )
        }

        else -> {
            val first = exercises.first()
            val last = exercises.last()
            val middle = exercises.subList(1, exercises.lastIndex) // 1..size-2

            result += TrainingListValue.FirstExercise(
                exerciseState = first,
                position = position,
                key = "first-${this.id}-${first.id}",
                indexInTraining = 1
            )

            // separator between first and first middle
            result += TrainingListValue.BetweenExercises(
                position = position,
                key = "between-${this.id}-${first.id}-${middle.first().id}"
            )

            middle.forEachIndexed { i, ex ->
                val indexInTraining = i + 2 // 2..(n-1)

                result += TrainingListValue.MiddleExercise(
                    exerciseState = ex,
                    position = position,
                    key = "middle-${this.id}-${ex.id}",
                    indexInTraining = indexInTraining
                )

                val next = middle.getOrNull(i + 1) ?: last
                result += TrainingListValue.BetweenExercises(
                    position = position,
                    key = "between-${this.id}-${ex.id}-${next.id}"
                )
            }

            result += TrainingListValue.LastExercise(
                exerciseState = last,
                position = position,
                key = "last-${this.id}-${last.id}",
                indexInTraining = exercises.size
            )
        }
    }

    return result.toPersistentList()
}