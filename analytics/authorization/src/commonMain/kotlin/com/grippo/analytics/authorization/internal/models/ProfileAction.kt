package com.grippo.analytics.authorization.internal.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode

internal enum class ProfileAction(override val analyticsCode: String) : AnalyticsCode {
    Create("create"),
    Logout("logout"),
}
