package com.grippo.toolkit.analytics.identity

import com.grippo.toolkit.analytics.schema.AnalyticsCode

public class AnalyticsUserPropertyKey<T : AnalyticsCode>(public val name: String) {
    public fun value(value: T): AnalyticsUserProperty =
        AnalyticsUserProperty(this, value.analyticsCode)
}
