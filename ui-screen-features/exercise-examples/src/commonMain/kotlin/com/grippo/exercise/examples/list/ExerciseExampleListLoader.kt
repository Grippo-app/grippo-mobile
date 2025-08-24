package com.grippo.exercise.examples.list

import androidx.compose.runtime.Immutable
import com.grippo.core.models.BaseLoader

@Immutable
internal sealed interface ExerciseExampleListLoader : BaseLoader {
    data object Loading : ExerciseExampleListLoader
}
