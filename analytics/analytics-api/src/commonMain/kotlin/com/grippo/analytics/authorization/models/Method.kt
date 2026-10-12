package com.grippo.analytics.authorization.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode

public enum class Method(public override val analyticsCode: String) : AnalyticsCode {
    Email("email"),
    Google("google"),
    Apple("apple")
}
