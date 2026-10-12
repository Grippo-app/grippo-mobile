package com.grippo.analytics.authorization.internal.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode

internal enum class InputField(override val analyticsCode: String) : AnalyticsCode {
    Email("email"),
    Password("password"),
    Name("name"),
    Height("height"),
    Weight("weight"),
}
