package com.grippo.toolkit.analytics.core.internal

internal enum class AnalyticsDisposition {
    Queued,
    AcceptedBySink,
    CollectionDisabled,
    QueueFull,
    InvalidEvent,
    InvalidIdentity,
    DiscardedOnPolicyChange,
    DiscardedOnIdentityChange,
    DiscardedBeforeSend,
    TransportFailure,
    Closed
}
