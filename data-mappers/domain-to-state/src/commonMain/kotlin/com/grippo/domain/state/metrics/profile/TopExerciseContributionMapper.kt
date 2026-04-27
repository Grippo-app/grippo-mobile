package com.grippo.domain.state.metrics.profile

import com.grippo.core.state.metrics.profile.TopExerciseContributionState
import com.grippo.data.features.api.metrics.profile.models.TopExerciseContribution
import com.grippo.domain.state.exercise.example.toState

public fun TopExerciseContribution.toState(): TopExerciseContributionState {
    return TopExerciseContributionState(
        exampleId = exampleId,
        name = name,
        totalSets = totalSets,
        stimulusShare = stimulusShare,
        heaviestWeight = heaviestWeight,
        estimatedOneRepMax = estimatedOneRepMax,
        category = category?.toState(),
    )
}
