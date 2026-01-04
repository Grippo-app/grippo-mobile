package com.grippo.domain.state.training.transformation

import com.grippo.core.state.metrics.MonthlyDigestState
import com.grippo.core.state.metrics.WeeklyDigestState
import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.core.state.trainings.TrainingState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.daysUntil

public fun List<TrainingState>.transformToTrainingListValue(
    range: DateRange,
    weeklyDigest: WeeklyDigestState? = null,
    monthlyDigest: MonthlyDigestState? = null,
): ImmutableList<TrainingListValue> {
    val nonEmpty = this.filter { it.exercises.isNotEmpty() }
    if (nonEmpty.isEmpty()) return persistentListOf()
    val days = range.from.date.daysUntil(range.to.date) + 1

    return when {
        days <= 1 -> nonEmpty.toDailyTrainingListValue()
        days <= 7 -> nonEmpty.toWeeklyTrainingListValue(
            summary = requireNotNull(weeklyDigest) {
                "Weekly digest is required for weekly timeline range."
            }
        )

        else -> nonEmpty.toMonthlyTrainingListValue(
            digest = requireNotNull(monthlyDigest) {
                "Monthly digest is required for monthly timeline range."
            }
        )
    }
}
