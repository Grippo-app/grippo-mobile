package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Immutable

@Immutable
public data class TrainingProfileInsightState(
    val severity: TrainingProfileInsightSeverityState,
    val reason: TrainingProfileInsightReasonState,
    val action: TrainingProfileInsightActionState? = null,
)
