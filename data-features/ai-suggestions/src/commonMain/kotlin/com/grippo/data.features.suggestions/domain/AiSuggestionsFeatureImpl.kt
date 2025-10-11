package com.grippo.data.features.suggestions.domain

import com.grippo.data.features.api.suggestion.AiSuggestionFeature
import com.grippo.data.features.api.suggestion.models.ExerciseExampleSuggestion
import org.koin.core.annotation.Single

@Single(binds = [AiSuggestionFeature::class])
internal class AiSuggestionsFeatureImpl(
    private val repository: AiSuggestionsRepository
) : AiSuggestionFeature {
    override suspend fun predictExerciseExample(): Result<ExerciseExampleSuggestion?> {
        return repository.predictExerciseExample()
    }
}
