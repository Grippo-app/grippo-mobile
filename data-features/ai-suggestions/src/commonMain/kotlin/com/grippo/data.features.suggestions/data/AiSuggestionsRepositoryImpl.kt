package com.grippo.data.features.suggestions.data

import com.grippo.data.features.api.suggestion.models.ExerciseExampleSuggestion
import com.grippo.data.features.suggestions.domain.AiSuggestionsRepository
import com.grippo.data.features.suggestions.prompt.exercise.example.ExerciseExampleSuggestionPromptBuilder
import org.koin.core.annotation.Single

@Single(binds = [AiSuggestionsRepository::class])
internal class AiSuggestionsRepositoryImpl(
    private val promptBuilder: ExerciseExampleSuggestionPromptBuilder
) : AiSuggestionsRepository {

    override suspend fun predictExerciseExample(): Result<ExerciseExampleSuggestion?> {
        return runCatching { // todo migrate runCatching in ApiAgentClient
            promptBuilder.suggest()
        }
    }
}
