package com.grippo.analytics.profile

import com.grippo.analytics.profile.models.ProfileEvent
import com.grippo.analytics.profile.models.ProfileOperation
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation

public interface ProfileAnalytics {
    public fun trackEvent(event: ProfileEvent)

    public suspend fun <T> trackOperation(operation: ProfileOperation, block: suspend () -> T): T

    public fun presentScreen(
        screen: ProfileScreen,
        reason: NavigationReason = NavigationReason.Initial,
    ): ScreenPresentation
}
