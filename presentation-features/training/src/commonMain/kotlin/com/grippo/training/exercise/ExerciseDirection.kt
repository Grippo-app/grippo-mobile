package com.grippo.training.exercise

import com.grippo.core.models.BaseDirection

internal sealed interface ExerciseDirection : BaseDirection {
    data object Back : ExerciseDirection
}
