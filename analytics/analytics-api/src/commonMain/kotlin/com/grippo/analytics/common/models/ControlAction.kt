package com.grippo.analytics.common.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode

public enum class ControlAction(public override val analyticsCode: String) : AnalyticsCode {
    Back("back"),
    Next("next"),
}
