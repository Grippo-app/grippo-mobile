package com.grippo.data.features.suggestions.domain

import com.grippo.data.features.api.suggestion.models.ExerciseExampleSuggestion

internal interface SuggestionsRepository {
    suspend fun predictExerciseExample(): Result<ExerciseExampleSuggestion?>
}
