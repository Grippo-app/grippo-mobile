package com.grippo.data.features.api.metrics.profile.models

public data class TrainingProfileInsight(
    val severity: TrainingProfileInsightSeverity,
    val reason: TrainingProfileInsightReason,
    val action: TrainingProfileInsightAction? = null,
)
