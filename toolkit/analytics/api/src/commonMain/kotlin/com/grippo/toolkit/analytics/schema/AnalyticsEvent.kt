package com.grippo.toolkit.analytics.schema

public interface AnalyticsEvent {
    public val schema: AnalyticsEventSchema
    public fun parameters(): List<AnalyticsParameter>
}
