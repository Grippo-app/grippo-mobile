package com.grippo.toolkit.analytics.schema

public interface AnalyticsEvent {
    public val schema: AnalyticsEventSchema
    /** Pure, bounded encoding. No IO, mutable shared state or analytics callbacks. */
    public fun parameters(): List<AnalyticsParameter>
}
