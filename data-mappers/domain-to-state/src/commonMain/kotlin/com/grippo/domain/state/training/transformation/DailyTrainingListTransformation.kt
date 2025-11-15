package com.grippo.domain.state.training.transformation

import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.core.state.trainings.TrainingPosition
import com.grippo.core.state.trainings.TrainingState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDate

internal fun List<TrainingState>.toDailyTrainingListValue(): ImmutableList<TrainingListValue> {
    val groupedByDate: Map<LocalDate, List<TrainingState>> = groupBy { training ->
        training.createdAt.date
    }

    val sortedDates = groupedByDate.keys.sortedDescending()

    val flat = buildList {
        sortedDates.forEach { date ->
            val trainingsForDate = groupedByDate.getValue(date)

            val digest = trainingsForDate.toDailyDigestState(date)
            add(
                TrainingListValue.DailyDigest(
                    state = digest,
                    key = "digest-$date",
                )
            )

            trainingsForDate.forEachIndexed { index, training ->
                val position = when {
                    trainingsForDate.size == 1 -> TrainingPosition.SINGLE
                    index == 0 -> TrainingPosition.FIRST
                    index == trainingsForDate.lastIndex -> TrainingPosition.LAST
                    else -> TrainingPosition.MIDDLE
                }

                add(
                    TrainingListValue.DateTime(
                        createAt = training.createdAt,
                        duration = training.duration,
                        position = position,
                        trainingId = training.id,
                        key = "date-${training.id}",
                    )
                )

                addAll(training.toTrainingListValues(position))
            }
        }
    }

    return flat.toPersistentList()
}
