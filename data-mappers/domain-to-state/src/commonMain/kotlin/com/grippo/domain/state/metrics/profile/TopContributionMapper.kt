package com.grippo.domain.state.metrics.profile

import com.grippo.core.state.metrics.profile.TopExerciseContributionState
import com.grippo.core.state.metrics.profile.TopMuscleContributionState
import com.grippo.core.state.metrics.profile.TopMuscleGroupContributionState
import com.grippo.data.features.api.metrics.profile.models.TopExerciseContribution
import com.grippo.data.features.api.metrics.profile.models.TopMuscleContribution
import com.grippo.data.features.api.metrics.profile.models.TopMuscleGroupContribution
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

public fun TopMuscleGroupContribution.toState(): TopMuscleGroupContributionState {
    return TopMuscleGroupContributionState(
        group = group.toState(),
        share = share,
    )
}