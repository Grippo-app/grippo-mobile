package com.grippo.data.features.api.metrics.profile.models

import com.grippo.data.features.api.muscle.models.MuscleGroupEnum

public data class TopMuscleGroupContribution(
    val group: MuscleGroupEnum,
    val share: Int,                  // % of period's total muscle work (0..100)
)
