package com.grippo.exercise.example.exerciseexample

import com.grippo.core.foundation.models.BaseDirection

public sealed interface ExerciseExampleDetailsDirection : BaseDirection {
    public data object Back : ExerciseExampleDetailsDirection
    public data object Action : ExerciseExampleDetailsDirection
}
