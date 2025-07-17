package com.grippo.exercise

import com.grippo.core.models.BaseDirection

public sealed interface ExerciseDirection : BaseDirection {
    public data class BackWithResult(val id: String) : ExerciseDirection
    public data object Back : ExerciseDirection
}