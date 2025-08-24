package com.grippo.exercise

import com.grippo.core.models.BaseDirection

public sealed interface ExerciseDirection : BaseDirection {
    public data object Back : ExerciseDirection
}