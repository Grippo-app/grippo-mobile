package com.grippo.domain.state.user

import com.grippo.core.state.profile.GoalState
import com.grippo.data.features.api.goal.models.Goal
import kotlinx.collections.immutable.toPersistentList

public fun Goal.toState(): GoalState {
    return GoalState(
        primaryGoal = primaryGoal.toState(),
        secondaryGoal = secondaryGoal?.toState(),
        target = target,
        personalizations = personalizations.map { it.toState() }.toPersistentList(),
    )
}
