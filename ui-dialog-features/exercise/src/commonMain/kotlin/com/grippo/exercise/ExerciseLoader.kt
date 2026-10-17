package com.grippo.exercise

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
public sealed interface ExerciseLoader : BaseLoader {
    @Immutable
    public data object History : ExerciseLoader
}
