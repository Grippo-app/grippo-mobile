package com.grippo.exercise

import com.grippo.core.foundation.models.BaseDirection

public sealed interface ExerciseDirection : BaseDirection {
    public data class ShowExampleDetails(val id: String) : ExerciseDirection
    public data object Back : ExerciseDirection
}