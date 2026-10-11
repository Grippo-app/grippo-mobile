package com.grippo.exercises.library

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.examples.ExerciseExampleState

internal sealed interface ExerciseLibraryDirection : BaseDirection {
    public data class BackWithResult(val value: ExerciseExampleState) : ExerciseLibraryDirection
    public data object Back : ExerciseLibraryDirection
}
