package com.grippo.toolkit.analytics.context

import kotlinx.serialization.Serializable

@Serializable
public data class NavigationContext(
    public val screen: ScreenId,
    public val referrer: ScreenId? = null,
    public val reason: NavigationReason,
    public val dialog: DialogId? = null,
)
