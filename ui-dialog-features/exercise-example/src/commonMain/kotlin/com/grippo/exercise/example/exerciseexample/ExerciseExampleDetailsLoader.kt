package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Immutable
import com.grippo.core.foundation.models.BaseLoader

@Immutable
public sealed interface ExerciseExampleDetailsLoader : BaseLoader {
    @Immutable
    public data object Example : ExerciseExampleDetailsLoader
    @Immutable
    public data object RecentExercises : ExerciseExampleDetailsLoader
    @Immutable
    public data object Achievements : ExerciseExampleDetailsLoader
}
