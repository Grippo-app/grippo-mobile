package com.grippo.toolkit.analytics.core.internal

internal data class AnalyticsDiagnostic(
    val disposition: AnalyticsDisposition,
    val eventName: String? = null,
)
