package com.grippo.domain.state.metrics

import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.metrics.GoalProgressState
import com.grippo.data.features.api.metrics.models.GoalAdherence
import com.grippo.domain.state.user.toState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.daysUntil

public fun GoalAdherence.toState(): GoalProgressState {
    val now = DateTimeUtils.now()
    val startDate = goal.createdAt.date
    val targetDate = goal.target.date

    val daysTotal = startDate.daysUntil(targetDate).coerceAtLeast(1)
    val daysElapsed = startDate.daysUntil(now.date).coerceIn(0, daysTotal)
    val daysRemaining = now.date.daysUntil(targetDate)

    return GoalProgressState(
        goal = goal.toState(),
        now = DateFormatState.of(
            value = now,
            range = DateRange.Range.Infinity().range,
            format = DateFormat.DateOnly.DateDdMmm
        ),
        score = score,
        strengthShare = strengthShare,
        hypertrophyShare = hypertrophyShare,
        enduranceShare = enduranceShare,
        daysTotal = daysTotal,
        daysElapsed = daysElapsed,
        daysRemaining = daysRemaining,
        progressFraction = (daysElapsed.toFloat() / daysTotal.toFloat()).coerceIn(0f, 1f),
        isFinished = now.date >= targetDate,
    )
}
