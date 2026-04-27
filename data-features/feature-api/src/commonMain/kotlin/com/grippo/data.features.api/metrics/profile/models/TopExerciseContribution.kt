package com.grippo.data.features.api.metrics.profile.models

import com.grippo.data.features.api.exercise.example.models.CategoryEnum

public data class TopExerciseContribution(
    val exampleId: String,
    val name: String,
    val totalSets: Int,
    val stimulusShare: Int,          // % of period's total stimulus (0..100)
    val heaviestWeight: Float,       // heaviest lift recorded in the range
    val estimatedOneRepMax: Float,   // p90 Epley e1RM (0 if not applicable)
    val category: CategoryEnum?,
)