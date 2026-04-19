package com.grippo.domain.state.metrics

import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.metrics.GoalAdherenceState
import com.grippo.core.state.metrics.GoalProgressState
import com.grippo.data.features.api.metrics.models.GoalAdherence
import com.grippo.domain.state.user.toState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils

public fun GoalAdherence.toState(): GoalProgressState {
    return GoalProgressState(
        goal = goal.toState(),
        adherence = GoalAdherenceState(
            score = score,
            strengthShare = strengthShare,
            hypertrophyShare = hypertrophyShare,
            enduranceShare = enduranceShare,
        ),
        now = DateFormatState.of(
            DateTimeUtils.now(),
            range = DateRange.Range.Infinity().range,
            format = DateFormat.DateOnly.DateMmmDdYyyy
        )
    )
}