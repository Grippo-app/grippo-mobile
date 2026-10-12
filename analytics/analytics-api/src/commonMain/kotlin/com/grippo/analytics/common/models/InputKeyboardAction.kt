package com.grippo.analytics.common.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode

public enum class InputKeyboardAction(public override val analyticsCode: String) : AnalyticsCode {
    Next("next"),
    Done("done"),
}
