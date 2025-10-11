package com.grippo.data.features.suggestions.data

import com.grippo.data.features.api.suggestion.models.ExerciseExampleSuggestion
import com.grippo.data.features.suggestions.domain.SuggestionsRepository
import com.grippo.data.features.suggestions.prompt.exercise.example.ExerciseExampleSuggestionPromptBuilder
import org.koin.core.annotation.Single

@Single(binds = [SuggestionsRepository::class])
internal class SuggestionsRepositoryImpl(
    private val promptBuilder: ExerciseExampleSuggestionPromptBuilder
) : SuggestionsRepository {

    override suspend fun predictExerciseExample(): Result<ExerciseExampleSuggestion?> {
        return runCatching { // todo migrate runCatching in ApiAgentClient
            promptBuilder.suggest()
        }
    }
}
