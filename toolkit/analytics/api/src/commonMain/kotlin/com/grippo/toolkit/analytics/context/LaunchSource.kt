package com.grippo.toolkit.analytics.context

import com.grippo.toolkit.analytics.schema.AnalyticsCode
import kotlinx.serialization.Serializable

@Serializable
public enum class LaunchSource(override val analyticsCode: String) : AnalyticsCode {
    Unknown("unknown"), Launcher("launcher"), Notification("notification"), DeepLink("deep_link")
}
