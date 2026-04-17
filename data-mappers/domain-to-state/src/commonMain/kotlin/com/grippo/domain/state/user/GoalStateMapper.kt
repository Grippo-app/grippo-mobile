package com.grippo.domain.state.user

import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.profile.GoalState
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.toPersistentList

public fun Goal.toState(): GoalState {
    return GoalState(
        primaryGoal = primaryGoal.toState(),
        secondaryGoal = secondaryGoal?.toState(),
        target = DateFormatState.of(target, range = DateRange.Range.Infinity().range),
        personalizations = personalizations.map { it.toState() }.toPersistentList(),
    )
}
