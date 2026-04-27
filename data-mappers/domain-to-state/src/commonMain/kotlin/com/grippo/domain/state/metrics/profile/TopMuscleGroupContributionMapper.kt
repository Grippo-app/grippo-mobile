package com.grippo.domain.state.metrics.profile

import com.grippo.core.state.metrics.profile.TopMuscleGroupContributionState
import com.grippo.data.features.api.metrics.profile.models.TopMuscleGroupContribution
import com.grippo.domain.state.muscles.toState

public fun TopMuscleGroupContribution.toState(): TopMuscleGroupContributionState {
    return TopMuscleGroupContributionState(
        group = group.toState(),
        share = share,
    )
}
