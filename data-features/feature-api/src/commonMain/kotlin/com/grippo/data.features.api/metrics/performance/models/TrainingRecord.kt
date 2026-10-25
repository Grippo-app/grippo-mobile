package com.grippo.data.features.api.metrics.performance.models

import com.grippo.data.features.api.achievements.Achievement
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue

public data class TrainingRecord(
    val exerciseExample: ExerciseExampleValue,
    val achievements: List<Achievement>,
)
