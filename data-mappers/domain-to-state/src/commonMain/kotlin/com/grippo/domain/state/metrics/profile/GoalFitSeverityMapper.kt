package com.grippo.domain.state.metrics.profile

import com.grippo.core.state.metrics.profile.GoalFitSeverityState
import com.grippo.data.features.api.metrics.profile.models.GoalFitSeverity

public fun GoalFitSeverity.toState(): GoalFitSeverityState = when (this) {
    GoalFitSeverity.PASS -> GoalFitSeverityState.PASS
    GoalFitSeverity.WARN -> GoalFitSeverityState.WARN
    GoalFitSeverity.FAIL -> GoalFitSeverityState.FAIL
}
