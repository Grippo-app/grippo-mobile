package com.grippo.exercise.example.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
public sealed interface ExerciseExamplePickerLoader : BaseLoader {
    @Immutable
    public data object SuggestExample : ExerciseExamplePickerLoader
}