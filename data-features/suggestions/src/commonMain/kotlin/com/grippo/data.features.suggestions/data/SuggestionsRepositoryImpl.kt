package com.grippo.data.features.suggestions.data

import com.grippo.ai.AiService
import com.grippo.data.features.api.suggestion.models.ExerciseExampleSuggestion
import com.grippo.data.features.suggestions.domain.SuggestionsRepository
import com.grippo.data.features.suggestions.prompt.exercise.example.ExerciseExampleSuggestionPromptBuilder
import com.grippo.database.dao.DraftTrainingDao
import com.grippo.database.dao.ExerciseExampleDao
import com.grippo.database.dao.TrainingDao
import com.grippo.database.dao.UserActiveDao
import com.grippo.database.dao.UserDao
import org.koin.core.annotation.Single

@Single(binds = [SuggestionsRepository::class])
internal class SuggestionsRepositoryImpl(
    private val aiService: AiService,
    private val draftTrainingDao: DraftTrainingDao,
    private val trainingDao: TrainingDao,
    private val exerciseExampleDao: ExerciseExampleDao,
    private val userDao: UserDao,
    private val userActiveDao: UserActiveDao,
) : SuggestionsRepository {

    private val promptBuilder = ExerciseExampleSuggestionPromptBuilder(
        aiService = aiService,
        draftTrainingDao = draftTrainingDao,
        trainingDao = trainingDao,
        exerciseExampleDao = exerciseExampleDao,
        userDao = userDao,
        userActiveDao = userActiveDao
    )

    override suspend fun predictExerciseExample(): Result<ExerciseExampleSuggestion?> {
        return runCatching {
            promptBuilder.suggest()
        }
    }
}
