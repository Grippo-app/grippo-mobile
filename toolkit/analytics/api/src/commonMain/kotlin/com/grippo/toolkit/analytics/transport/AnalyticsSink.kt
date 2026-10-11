package com.grippo.toolkit.analytics.transport

import com.grippo.toolkit.analytics.lifecycle.AnalyticsCollection

/**
 * Synchronous, non-blocking SDK submission; core invokes methods serially.
 * Returning means accepted by SDK, not delivered remotely. Exceptions report transport failures.
 */
public interface AnalyticsSink {
    public fun setCollection(collection: AnalyticsCollection)
    public fun setIdentity(identity: AnalyticsIdentityPayload)
    public fun send(payload: AnalyticsPayload)
}
