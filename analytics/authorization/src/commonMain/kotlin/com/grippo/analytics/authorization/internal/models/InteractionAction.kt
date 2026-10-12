package com.grippo.analytics.authorization.internal.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode

internal enum class InteractionAction(override val analyticsCode: String) : AnalyticsCode {
    Back("back"),
    Next("next"),
    Register("register"),
    GroupSelected("group_selected"),
}
