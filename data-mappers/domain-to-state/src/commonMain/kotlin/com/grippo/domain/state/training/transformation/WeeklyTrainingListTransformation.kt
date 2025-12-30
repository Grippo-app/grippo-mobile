package com.grippo.domain.state.training.transformation

import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.core.state.trainings.TrainingPosition
import com.grippo.core.state.trainings.TrainingState
import com.grippo.domain.state.metrics.toWeeklyDigestState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDate

internal fun List<TrainingState>.toWeeklyTrainingListValue(
    range: DateRange?,
): ImmutableList<TrainingListValue> {
    val summary = toWeeklyDigestState(range)
    val groupedByDate: Map<LocalDate, List<TrainingState>> =
        sortedByDescending { it.createdAt }.groupBy { it.createdAt.date }
    val sortedDates = groupedByDate.keys.sortedDescending()

    val flat = mutableListOf<TrainingListValue>()
    flat += TrainingListValue.WeeklySummary(
        summary = summary,
        key = "weekly-summary-${summary.weekStart}",
    )

    sortedDates.forEachIndexed { index, date ->
        val trainingsForDate = groupedByDate.getValue(date)
        val position = when {
            sortedDates.size == 1 -> TrainingPosition.SINGLE
            index == 0 -> TrainingPosition.FIRST
            index == sortedDates.lastIndex -> TrainingPosition.LAST
            else -> TrainingPosition.MIDDLE
        }

        flat += TrainingListValue.WeeklyTrainingsDay(
            date = date,
            trainings = trainingsForDate.sortedByDescending { it.createdAt }.toPersistentList(),
            position = position,
            key = "weekly-day-$date",
        )
    }

    return flat.toPersistentList()
}
