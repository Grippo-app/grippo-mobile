package com.grippo.data.features.api.metrics.models

import com.grippo.data.features.api.muscle.models.MuscleEnum

public data class MuscleLoadSummary(
    val perGroup: MuscleLoadBreakdown,
    val perMuscle: MuscleLoadBreakdown,
)

public data class MuscleLoadBreakdown(
    val entries: List<MuscleLoadEntry>,
)

public data class MuscleLoadEntry(
    val label: String,
    val percentage: Float,
    val muscles: List<MuscleEnum>,
)
