package com.grippo.domain.state.metrics.profile

import com.grippo.core.state.metrics.profile.TopMuscleContributionState
import com.grippo.data.features.api.metrics.profile.models.TopMuscleContribution
import com.grippo.domain.state.muscles.toState

public fun TopMuscleContribution.toState(): TopMuscleContributionState {
    return TopMuscleContributionState(
        muscle = muscle.toState(),
        share = share,
    )
}
