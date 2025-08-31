package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Immutable
import com.grippo.state.exercise.examples.ExerciseExampleDialogView
import com.grippo.state.exercise.examples.ExerciseExampleState

@Immutable
public data class ExerciseExampleState(
    val example: ExerciseExampleState? = null,
    val view: ExerciseExampleDialogView,
)