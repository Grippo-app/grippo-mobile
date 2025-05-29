package com.grippo.exercise.example.exerciseexample

import com.grippo.core.models.BaseDirection

public sealed interface ExerciseExampleDirection : BaseDirection {
    public data class DismissWithResult(val id: String) : ExerciseExampleDirection
}