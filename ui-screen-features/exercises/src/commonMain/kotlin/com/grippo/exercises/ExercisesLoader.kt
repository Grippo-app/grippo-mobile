package com.grippo.exercises

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
public sealed interface ExercisesLoader : BaseLoader {
    @Immutable
    public data object Loading : ExercisesLoader
}
