package com.grippo.toolkit.analytics.schema

import com.grippo.toolkit.analytics.identity.AnalyticsUserPropertyKey

/** Each feature can contribute one named Koin binding without depending on analytics core. */
public class AnalyticsSchema(
    events: List<AnalyticsEventSchema>,
    properties: List<AnalyticsUserPropertyKey<*>> = emptyList(),
) {
    public val events: List<AnalyticsEventSchema> = events.toList()
    public val properties: List<AnalyticsUserPropertyKey<*>> = properties.toList()
}
