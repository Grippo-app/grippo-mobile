package com.grippo.exercises

import com.grippo.core.foundation.models.BaseDirection

public sealed interface ExercisesRootDirection : BaseDirection {
    public data object Back : ExercisesRootDirection
}
