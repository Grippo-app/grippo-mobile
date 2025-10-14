package com.grippo.domain.state.exercise.example

import com.grippo.core.state.examples.ExerciseExampleBundleState
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleBundle
import com.grippo.domain.state.muscles.toState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<ExerciseExampleBundle>.toState(): ImmutableList<ExerciseExampleBundleState> {
    return map { it.toState() }.toPersistentList()
}

public fun ExerciseExampleBundle.toState(): ExerciseExampleBundleState {
    return ExerciseExampleBundleState(
        id = id,
        percentage = PercentageFormatState.of(percentage),
        muscle = muscle.toState().value
    )
}