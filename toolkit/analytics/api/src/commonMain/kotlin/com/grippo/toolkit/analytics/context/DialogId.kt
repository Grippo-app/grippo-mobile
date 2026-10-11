package com.grippo.toolkit.analytics.context

import com.grippo.toolkit.analytics.internal.requireCode
import com.grippo.toolkit.analytics.schema.AnalyticsCode
import kotlinx.serialization.Serializable

@Serializable
public data class DialogId(override val analyticsCode: String) : AnalyticsCode {
    init {
        requireCode(analyticsCode)
    }
}
