package com.grippo.data.features.api.training.models

import kotlinx.datetime.LocalDateTime

/** Validation needs raw sets, not computed exercise totals or a fully saveable draft. */
public data class ExerciseValidationInput(
    val name: String,
    val exampleId: String,
    val iterations: List<SetIteration>,
    /** When unknown, only local/component checks can be made. */
    val performedAt: LocalDateTime?,
)
