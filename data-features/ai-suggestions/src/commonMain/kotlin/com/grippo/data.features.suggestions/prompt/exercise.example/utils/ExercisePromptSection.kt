package com.grippo.data.features.suggestions.prompt.exercise.example.utils

internal interface ExercisePromptSection {
    val id: String
    val description: String

    fun render(into: StringBuilder)
}