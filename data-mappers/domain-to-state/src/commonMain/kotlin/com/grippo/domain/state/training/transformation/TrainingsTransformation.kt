package com.grippo.domain.state.training.transformation

import com.grippo.state.trainings.TrainingListValue
import com.grippo.state.trainings.TrainingPosition
import com.grippo.state.trainings.TrainingState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<TrainingState>.transformToTrainingListValue(): ImmutableList<TrainingListValue> {
    val nonEmpty = this.filter { it.exercises.isNotEmpty() }

    val flat = buildList {
        nonEmpty.forEachIndexed { index, training ->
            val position = when {
                nonEmpty.size == 1 -> TrainingPosition.SINGLE
                index == 0 -> TrainingPosition.FIRST
                index == nonEmpty.lastIndex -> TrainingPosition.LAST
                else -> TrainingPosition.MIDDLE
            }

            add(
                TrainingListValue.DateTime(
                    date = training.createdAt,
                    position = position,
                    id = "date-${training.id}",
                    volume = training.metrics.volume,
                    repetitions = training.metrics.repetitions
                )
            )

            addAll(training.toTrainingListValues(position))
        }
    }

    return flat.toPersistentList()
}