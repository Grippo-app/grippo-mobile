package com.grippo.exercise.examples.list

import com.grippo.core.models.BaseDirection

internal sealed interface ExerciseExampleListDirection : BaseDirection {
    data object Back : ExerciseExampleListDirection
}
