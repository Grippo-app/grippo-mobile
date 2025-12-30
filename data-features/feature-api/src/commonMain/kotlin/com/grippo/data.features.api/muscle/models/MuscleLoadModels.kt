package com.grippo.data.features.api.muscle.models

public data class MuscleLoadEntry(
    val label: String,
    val percentage: Float,
    val muscles: List<MuscleEnum>,
)

public data class MuscleLoadBreakdown(
    val entries: List<MuscleLoadEntry>,
)

public data class MuscleLoadSummary(
    val perGroup: MuscleLoadBreakdown,
    val perMuscle: MuscleLoadBreakdown,
)
