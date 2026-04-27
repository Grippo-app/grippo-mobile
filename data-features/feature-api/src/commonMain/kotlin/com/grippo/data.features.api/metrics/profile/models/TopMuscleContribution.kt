package com.grippo.data.features.api.metrics.profile.models

import com.grippo.data.features.api.muscle.models.MuscleEnum

public data class TopMuscleContribution(
    val muscle: MuscleEnum,
    val share: Int,                  // % of period's total muscle work (0..100)
)