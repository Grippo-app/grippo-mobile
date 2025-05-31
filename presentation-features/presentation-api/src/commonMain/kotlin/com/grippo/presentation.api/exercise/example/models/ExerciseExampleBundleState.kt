package com.grippo.presentation.api.exercise.example.models

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.muscles.models.MuscleState
import com.grippo.presentation.api.muscles.models.stubMuscles
import kotlin.random.Random
import kotlin.uuid.Uuid

@Immutable
public data class ExerciseExampleBundleState(
    val id: String,
    val muscle: MuscleState,
    val percentage: Int
)

public fun stubExerciseExampleBundle(): ExerciseExampleBundleState {
    return ExerciseExampleBundleState(
        id = Uuid.random().toString(),
        muscle = stubMuscles().random().muscles.random().value,
        percentage = Random.nextInt(5, 60)
    )
}