package com.grippo.domain.state.training.transformation

import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.core.state.trainings.TrainingPosition
import com.grippo.core.state.trainings.TrainingState
import com.grippo.domain.state.metrics.toMonthlyDigestState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

internal fun List<TrainingState>.toMonthlyTrainingListValue(
    range: DateRange?,
): ImmutableList<TrainingListValue> {
    val groupedByDate: Map<LocalDate, List<TrainingState>> = groupBy { training ->
        training.createdAt.date
    }
    val sortedDates = groupedByDate.keys.sortedDescending()
    val digest = toMonthlyDigestState(range)
    val flat = mutableListOf<TrainingListValue>()

    flat += TrainingListValue.MonthlyDigest(
        summary = digest,
        month = digest.month,
        key = "monthly-digest-${digest.month.year}-${digest.month.month.number}",
    )

    sortedDates.forEachIndexed { index, date ->
        val trainingsForDate = groupedByDate.getValue(date).sortedBy { it.createdAt }
        val position = when {
            sortedDates.size == 1 -> TrainingPosition.SINGLE
            index == 0 -> TrainingPosition.FIRST
            index == sortedDates.lastIndex -> TrainingPosition.LAST
            else -> TrainingPosition.MIDDLE
        }

        val monthReference = LocalDate(date.year, date.month, 1)
        flat += TrainingListValue.MonthlyTrainingsDay(
            date = date,
            month = monthReference,
            trainings = trainingsForDate.toPersistentList(),
            position = position,
            key = "monthly-day-$date",
        )
    }

    return flat.toPersistentList()
}
