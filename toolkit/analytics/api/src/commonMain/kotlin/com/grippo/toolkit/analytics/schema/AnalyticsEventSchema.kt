package com.grippo.toolkit.analytics.schema

public class AnalyticsEventSchema(
    public val name: String,
    public val version: Int,
    parameters: List<AnalyticsParameterKey<*>>,
) {
    public val parameters: List<AnalyticsParameterKey<*>> = parameters.toList()
}
