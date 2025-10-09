package com.grippo.data.features.suggestions.domain

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue

internal interface SuggestionsRepository {
    suspend fun predictExerciseExample(): ExerciseExampleValue?
}
