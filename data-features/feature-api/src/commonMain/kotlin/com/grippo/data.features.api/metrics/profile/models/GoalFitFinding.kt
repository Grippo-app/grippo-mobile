package com.grippo.data.features.api.metrics.profile.models

import com.grippo.data.features.api.muscle.models.MuscleGroupEnum

public data class GoalFitFinding(
    val rule: GoalFitRule,
    val severity: GoalFitSeverity,
    val actualValue: Float,
    val targetMin: Float?,
    val targetMax: Float?,
    val context: List<MuscleGroupEnum> = emptyList(),
)
