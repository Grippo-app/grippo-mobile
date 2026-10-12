package com.grippo.analytics.authorization.internal.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode

internal enum class AuthAction(override val analyticsCode: String) : AnalyticsCode {
    Login("login"),
    Register("register")
}
