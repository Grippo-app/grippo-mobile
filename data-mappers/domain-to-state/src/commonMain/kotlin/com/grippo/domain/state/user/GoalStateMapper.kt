package com.grippo.domain.state.user

import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.profile.GoalState
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.toPersistentList

public fun Goal.toState(): GoalState {
    return GoalState(
        primaryGoal = primaryGoal.toState(),
        secondaryGoal = secondaryGoal?.toState(),
        target = DateFormatState.of(
            value = target,
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy
        ),
        createdAt = DateFormatState.of(
            value = createdAt,
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy
        ),
        personalizations = personalizations.map { it.toState() }.toPersistentList(),
    )
}
