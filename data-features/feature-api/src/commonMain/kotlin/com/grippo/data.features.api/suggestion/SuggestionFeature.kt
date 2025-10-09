package com.grippo.data.features.api.suggestion

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue

public interface SuggestionFeature {
    public suspend fun predictExerciseExample(): ExerciseExampleValue?
}
