package com.grippo.toolkit.analytics

/** App startup contract. Feature code injects the consumer API instead. */
public interface AnalyticsBootstrap {
    /** Connects exactly once; duplicate connection or connection after close fails. */
    public fun attachSink(analyticsSink: AnalyticsSink)
}
