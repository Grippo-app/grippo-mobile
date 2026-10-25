package com.grippo.toolkit.analytics.transport

import com.grippo.toolkit.analytics.lifecycle.AnalyticsCollection

public interface AnalyticsSink {
    public fun setCollection(collection: AnalyticsCollection)
    public fun setIdentity(identity: AnalyticsIdentityPayload)
    public fun send(payload: AnalyticsPayload)
}
