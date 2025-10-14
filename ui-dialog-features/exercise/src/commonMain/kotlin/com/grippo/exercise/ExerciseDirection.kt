package com.grippo.exercise

import com.grippo.core.foundation.models.BaseDirection

public sealed interface ExerciseDirection : BaseDirection {
    public data object Back : ExerciseDirection
}