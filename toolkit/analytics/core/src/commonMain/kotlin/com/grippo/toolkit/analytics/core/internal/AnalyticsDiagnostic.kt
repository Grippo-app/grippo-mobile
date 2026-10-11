package com.grippo.toolkit.analytics.core.internal

/** Contains names/status only. No values, identifiers, exception messages or stack traces. */
internal data class AnalyticsDiagnostic(
    val disposition: AnalyticsDisposition,
    val eventName: String? = null,
)
