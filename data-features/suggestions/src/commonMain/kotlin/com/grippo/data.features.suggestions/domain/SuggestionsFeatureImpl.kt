package com.grippo.data.features.suggestions.domain

import com.grippo.data.features.api.suggestion.SuggestionFeature
import com.grippo.data.features.api.suggestion.models.ExerciseExampleSuggestion
import org.koin.core.annotation.Single

@Single(binds = [SuggestionFeature::class])
internal class SuggestionsFeatureImpl(
    private val repository: SuggestionsRepository
) : SuggestionFeature {
    override suspend fun predictExerciseExample(): Result<ExerciseExampleSuggestion?> {
        return repository.predictExerciseExample()
    }
}
