package com.grippo.analytics.profile

import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation

/** Stateless screen telemetry; presentation lifetime is owned by foundation. */
public interface ProfileAnalytics {
    public fun presentScreen(
        screen: ProfileScreen,
        reason: NavigationReason = NavigationReason.Initial,
    ): ScreenPresentation
}
