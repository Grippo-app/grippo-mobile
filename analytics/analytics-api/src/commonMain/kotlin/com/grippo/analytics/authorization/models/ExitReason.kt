package com.grippo.analytics.authorization.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode

public enum class ExitReason(public override val analyticsCode: String) : AnalyticsCode {
    Authenticated("authenticated"),
    ProfileRequired("profile_required"),
    UserBack("user_back")
}
