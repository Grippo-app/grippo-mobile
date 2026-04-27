package com.grippo.data.features.api.metrics.profile.models

public data class GoalFitFinding(
    val rule: GoalFitRule,
    val severity: GoalFitSeverity,
    val actualValue: Float,
    val targetMin: Float?,
    val targetMax: Float?,
    val context: List<String> = emptyList(),
)