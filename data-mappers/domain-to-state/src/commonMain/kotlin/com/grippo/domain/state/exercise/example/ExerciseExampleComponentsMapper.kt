package com.grippo.domain.state.exercise.example

import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleComponents

public fun ExerciseExampleComponents.toState(): ExerciseExampleComponentsState {
    return when (this) {
        is ExerciseExampleComponents.External -> ExerciseExampleComponentsState.External(
            required = required
        )

        is ExerciseExampleComponents.BodyOnly -> ExerciseExampleComponentsState.BodyOnly(
            required = required,
            bodyMultiplier = multiplier
        )

        is ExerciseExampleComponents.BodyAndExtra -> ExerciseExampleComponentsState.BodyAndExtra(
            bodyRequired = bodyRequired,
            bodyMultiplier = bodyMultiplier,
            extraRequired = extraRequired
        )

        is ExerciseExampleComponents.BodyAndAssist -> ExerciseExampleComponentsState.BodyAndAssist(
            bodyRequired = bodyRequired,
            bodyMultiplier = bodyMultiplier,
            assistRequired = assistRequired
        )
    }
}
