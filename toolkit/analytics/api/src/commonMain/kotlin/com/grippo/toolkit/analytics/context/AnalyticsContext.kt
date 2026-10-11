package com.grippo.toolkit.analytics.context

import kotlinx.serialization.Serializable

@Serializable
public data class AnalyticsContext(
    public val navigation: NavigationContext? = null,
    public val launchSource: LaunchSource = LaunchSource.Unknown,
)
