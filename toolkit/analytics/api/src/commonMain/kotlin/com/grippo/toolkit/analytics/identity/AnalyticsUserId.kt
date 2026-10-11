package com.grippo.toolkit.analytics.identity

import com.grippo.toolkit.analytics.internal.opaqueIdPattern

/** Opaque backend identifier. Never email, name, token, or a hash of these. */
public data class AnalyticsUserId(public val value: String) {
    init {
        require(opaqueIdPattern.matches(value))
    }
}
