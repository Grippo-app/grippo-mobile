package com.grippo.exercise.example.exerciseexample

import com.grippo.core.models.BaseDirection
import com.grippo.state.exercise.examples.ExerciseExampleState

public sealed interface ExerciseExampleDirection : BaseDirection {
    public data class BackWithResult(val value: ExerciseExampleState) : ExerciseExampleDirection
    public data object Back : ExerciseExampleDirection
}