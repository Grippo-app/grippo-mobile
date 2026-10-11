package com.grippo.data.features.api.training.models

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleComponents

/** Loaded evidence for synchronous validation. Missing history or body measurements are valid. */
public data class ExerciseValidationContext(
    val components: ExerciseExampleComponents? = null,
    val history: List<Exercise> = emptyList(),
    /** Excludes the persisted exercise when editing an existing session. */
    val excludedExerciseId: String? = null,
    /** Recorded weight near the exercise date; callers must omit stale measurements. */
    val bodyWeight: Float? = null,
)
