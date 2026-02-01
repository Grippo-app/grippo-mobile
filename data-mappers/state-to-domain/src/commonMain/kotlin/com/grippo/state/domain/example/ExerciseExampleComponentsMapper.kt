package com.grippo.state.domain.example

import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleComponents

public fun ExerciseExampleComponentsState.toDomain(): ExerciseExampleComponents {
    return when (this) {
        is ExerciseExampleComponentsState.External ->
            ExerciseExampleComponents.External(required)

        is ExerciseExampleComponentsState.BodyOnly ->
            ExerciseExampleComponents.BodyOnly(
                required = required,
                multiplier = multiplier
            )

        is ExerciseExampleComponentsState.BodyAndExtra ->
            ExerciseExampleComponents.BodyAndExtra(
                bodyRequired = bodyRequired,
                bodyMultiplier = bodyMultiplier,
                extraRequired = extraRequired
            )

        is ExerciseExampleComponentsState.BodyAndAssist ->
            ExerciseExampleComponents.BodyAndAssist(
                bodyRequired = bodyRequired,
                bodyMultiplier = bodyMultiplier,
                assistRequired = assistRequired
            )
    }
}
