package com.grippo.toolkit.analytics.context

import com.grippo.toolkit.analytics.schema.AnalyticsCode
import kotlinx.serialization.Serializable

@Serializable
public enum class NavigationReason(override val analyticsCode: String) : AnalyticsCode {
    Initial("initial"),
    Restore("restore"),
}
