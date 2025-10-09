package com.grippo.data.features.suggestions.data

import com.grippo.ai.AiService
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.suggestions.domain.SuggestionsRepository
import org.koin.core.annotation.Single

@Single(binds = [SuggestionsRepository::class])
internal class SuggestionsRepositoryImpl(
    private val aiService: AiService
) : SuggestionsRepository {
    override suspend fun predictExerciseExample(): ExerciseExampleValue? {
        val result = aiService.ask("Hello world")
        println("RESULT = $result")
        return null
    }
}