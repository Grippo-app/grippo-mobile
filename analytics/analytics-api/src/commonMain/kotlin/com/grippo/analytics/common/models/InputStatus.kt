package com.grippo.analytics.common.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode

/** Bounded validation facts; original input values never cross the analytics boundary. */
public enum class InputStatus(public override val analyticsCode: String) : AnalyticsCode {
    Empty("empty"),
    Invalid("invalid"),
    Valid("valid"),
}
