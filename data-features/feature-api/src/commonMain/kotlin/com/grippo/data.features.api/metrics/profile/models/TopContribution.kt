package com.grippo.data.features.api.metrics.profile.models

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.muscle.models.MuscleEnum

/**
 * A single exercise's contribution to the overall training stimulus in a
 * period. Shared by goal-adherence diagnostics and the training-profile
 * summary. Lists of these are sorted by [stimulusShare] descending.
 */
public data class TopExerciseContribution(
    val exampleId: String,
    val name: String,
    val totalSets: Int,
    val stimulusShare: Int,          // % of period's total stimulus (0..100)
    val heaviestWeight: Float,       // heaviest lift recorded in the range
    val estimatedOneRepMax: Float,   // p90 Epley e1RM (0 if not applicable)
    val category: CategoryEnum?,
)

/**
 * A single muscle's contribution to the overall training stimulus in a
 * period. Shared by goal-adherence diagnostics and the training-profile
 * summary. Lists of these are sorted by [share] descending.
 */
public data class TopMuscleContribution(
    val muscle: MuscleEnum,
    val share: Int,                  // % of period's total muscle work (0..100)
)
