package com.grippo.toolkit.analytics.identity

import com.grippo.toolkit.analytics.internal.opaqueIdPattern

public data class AnalyticsUserId(public val value: String) {
    init {
        require(opaqueIdPattern.matches(value))
    }
}
