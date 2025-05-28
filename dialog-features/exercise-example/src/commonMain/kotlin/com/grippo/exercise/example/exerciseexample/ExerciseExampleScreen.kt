package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Composable
import com.grippo.core.BaseComposeScreen
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun ExerciseExampleScreen(
    state: ExerciseExampleState,
    loaders: ImmutableSet<ExerciseExampleLoader>,
    contract: ExerciseExampleContract
) = BaseComposeScreen {
}