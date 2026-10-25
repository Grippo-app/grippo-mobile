package com.grippo.toolkit.analytics.lifecycle

import com.grippo.toolkit.analytics.transport.AnalyticsSink

public interface AnalyticsBootstrap {
    public fun attachSink(analyticsSink: AnalyticsSink)
}
