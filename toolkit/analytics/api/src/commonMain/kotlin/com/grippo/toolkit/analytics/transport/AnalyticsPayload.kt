package com.grippo.toolkit.analytics.transport

public class AnalyticsPayload(public val name: String, parameters: Map<String, AnalyticsValue>) {
    public val parameters: Map<String, AnalyticsValue> = parameters.toMap()
}
