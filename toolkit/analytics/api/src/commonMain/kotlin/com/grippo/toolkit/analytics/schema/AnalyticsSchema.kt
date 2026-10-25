package com.grippo.toolkit.analytics.schema

import com.grippo.toolkit.analytics.identity.AnalyticsUserPropertyKey

public class AnalyticsSchema(
    events: List<AnalyticsEventSchema>,
    properties: List<AnalyticsUserPropertyKey<*>> = emptyList(),
) {
    public val events: List<AnalyticsEventSchema> = events.toList()
    public val properties: List<AnalyticsUserPropertyKey<*>> = properties.toList()
}
