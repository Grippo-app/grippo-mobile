package com.grippo.analytics.authorization.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode

public enum class ProfilePicker(public override val analyticsCode: String) : AnalyticsCode {
    Height("height"),
    Weight("weight"),
}
