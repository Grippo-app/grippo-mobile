package com.grippo.domain.mapper.exercise.example

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleBundle
import com.grippo.domain.mapper.muscles.toState
import com.grippo.state.exercise.examples.ExerciseExampleBundleState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<ExerciseExampleBundle>.toState(): ImmutableList<ExerciseExampleBundleState> {
    return map { it.toState() }.toPersistentList()
}

public fun ExerciseExampleBundle.toState(): ExerciseExampleBundleState {
    return ExerciseExampleBundleState(
        id = id,
        percentage = percentage,
        muscle = muscle.toState().value
    )
}