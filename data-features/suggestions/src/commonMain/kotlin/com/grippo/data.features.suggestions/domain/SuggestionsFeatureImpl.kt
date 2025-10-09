package com.grippo.data.features.suggestions.domain

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.api.suggestion.SuggestionFeature
import org.koin.core.annotation.Single

@Single(binds = [SuggestionFeature::class])
internal class SuggestionsFeatureImpl(
    private val repository: SuggestionsRepository
) : SuggestionFeature {
    override suspend fun predictExerciseExample(): ExerciseExampleValue? {
        return repository.predictExerciseExample()
    }
}
