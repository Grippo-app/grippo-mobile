package com.grippo.toolkit.analytics.core

public enum class AnalyticsDisposition {
    Queued, AcceptedBySink, CollectionDisabled, QueueFull, InvalidEvent, InvalidIdentity,
    DiscardedOnPolicyChange, DiscardedOnIdentityChange, TransportFailure, Closed
}

/** Contains names/status only. No values, identifiers, exception messages or stack traces. */
public data class AnalyticsDiagnostic(
    public val disposition: AnalyticsDisposition,
    public val eventName: String? = null,
)

public enum class AnalyticsValidationMode { Reject, Throw }

internal expect class AnalyticsLock() {
    fun <T> withLock(block: () -> T): T
}
