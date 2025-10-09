package com.grippo.data.features.api.suggestion

import com.grippo.data.features.api.suggestion.models.ExerciseExampleSuggestion

public interface SuggestionFeature {
    public suspend fun predictExerciseExample(): Result<ExerciseExampleSuggestion?>
}
