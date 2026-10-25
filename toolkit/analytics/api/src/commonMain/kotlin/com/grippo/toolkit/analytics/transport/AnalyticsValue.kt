package com.grippo.toolkit.analytics.transport

public sealed class AnalyticsValue {
    public data class Text(public val value: String) : AnalyticsValue()
    public data class Integer(public val value: Long) : AnalyticsValue()
    public data class Decimal(public val value: Double) : AnalyticsValue()
}
