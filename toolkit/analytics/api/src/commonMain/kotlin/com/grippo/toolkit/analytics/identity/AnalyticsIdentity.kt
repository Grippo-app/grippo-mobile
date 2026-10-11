package com.grippo.toolkit.analytics.identity


public class AnalyticsIdentity(
    public val userId: AnalyticsUserId? = null,
    properties: List<AnalyticsUserProperty> = emptyList(),
) {
    public val properties: List<AnalyticsUserProperty> = properties.toList()
}
