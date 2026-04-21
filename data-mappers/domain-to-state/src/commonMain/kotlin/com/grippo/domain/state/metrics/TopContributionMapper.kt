package com.grippo.domain.state.metrics

import com.grippo.core.state.metrics.TopExerciseContributionState
import com.grippo.core.state.metrics.TopMuscleContributionState
import com.grippo.data.features.api.metrics.models.TopExerciseContribution
import com.grippo.data.features.api.metrics.models.TopMuscleContribution
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.toState

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

public fun TopMuscleContribution.toState(): TopMuscleContributionState {
    return TopMuscleContributionState(
        muscle = muscle.toState(),
        share = share,
    )
}