package com.grippo.toolkit.analytics.transport

/** Transport representation; raw maps never cross the feature API. */
public sealed class AnalyticsValue {
    public data class Text(public val value: String) : AnalyticsValue()
    public data class Integer(public val value: Long) : AnalyticsValue()
    public data class Decimal(public val value: Double) : AnalyticsValue()
}
