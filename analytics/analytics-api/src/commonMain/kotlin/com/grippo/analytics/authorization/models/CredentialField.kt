package com.grippo.analytics.authorization.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode

public enum class CredentialField(public override val analyticsCode: String) : AnalyticsCode {
    Email("email"),
    Password("password"),
}
