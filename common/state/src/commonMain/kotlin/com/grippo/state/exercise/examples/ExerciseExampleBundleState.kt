package com.grippo.state.exercise.examples

import androidx.compose.runtime.Immutable
import com.grippo.state.formatters.PercentageFormatState
import com.grippo.state.muscles.MuscleState
import com.grippo.state.muscles.stubMuscles
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlin.random.Random
import kotlin.uuid.Uuid

@Immutable
public data class ExerciseExampleBundleState(
    val id: String,
    val muscle: MuscleState,
    val percentage: PercentageFormatState
)

public fun stubExerciseExampleBundles(): ImmutableList<ExerciseExampleBundleState> {
    return stubMuscles().flatMap { it.muscles }.map { m ->
        ExerciseExampleBundleState(
            id = Uuid.random().toString(),
            muscle = m.value,
            percentage = PercentageFormatState.of(Random.nextInt(1, 20))
        )
    }.toPersistentList()
}

public fun stubExerciseExampleBundle(): ExerciseExampleBundleState {
    return ExerciseExampleBundleState(
        id = Uuid.random().toString(),
        muscle = stubMuscles().random().muscles.random().value,
        percentage = PercentageFormatState.of(Random.nextInt(1, 20))
    )
}