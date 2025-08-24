package com.grippo.exercise.examples

import com.grippo.core.models.BaseDirection

public sealed interface ExerciseExamplesDirection : BaseDirection {
    public data object Back : ExerciseExamplesDirection
    public data object Close : ExerciseExamplesDirection
}
