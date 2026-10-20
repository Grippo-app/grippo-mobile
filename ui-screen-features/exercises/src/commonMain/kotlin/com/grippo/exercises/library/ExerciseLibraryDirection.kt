package com.grippo.exercises.library

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.foundation.models.Result
import com.grippo.core.foundation.models.ResultKey
import com.grippo.core.state.examples.ExerciseExampleState

internal sealed interface ExerciseLibraryDirection : BaseDirection {
    data class BackWithResult(
        val resultKey: ResultKey<Result<ExerciseExampleState>>,
        val value: ExerciseExampleState,
    ) : ExerciseLibraryDirection

    data object Back : ExerciseLibraryDirection
}
