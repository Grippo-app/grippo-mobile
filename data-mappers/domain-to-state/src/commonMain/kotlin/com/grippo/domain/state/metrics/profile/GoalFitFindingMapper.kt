package com.grippo.domain.state.metrics.profile

import com.grippo.core.state.metrics.profile.GoalFitFindingState
import com.grippo.data.features.api.metrics.profile.models.GoalFitFinding
import com.grippo.domain.state.muscles.toState
import kotlinx.collections.immutable.toPersistentList

public fun GoalFitFinding.toState(): GoalFitFindingState = GoalFitFindingState(
    rule = rule.toState(),
    severity = severity.toState(),
    actualValue = actualValue,
    targetMin = targetMin,
    targetMax = targetMax,
    context = context.map { it.toState() }.toPersistentList(),
)
