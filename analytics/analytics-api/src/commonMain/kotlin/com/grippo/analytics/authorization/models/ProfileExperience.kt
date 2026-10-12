package com.grippo.analytics.authorization.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode

public enum class ProfileExperience(public override val analyticsCode: String) : AnalyticsCode {
    Beginner("beginner"),
    Intermediate("intermediate"),
    Advanced("advanced"),
    Pro("pro"),
}
