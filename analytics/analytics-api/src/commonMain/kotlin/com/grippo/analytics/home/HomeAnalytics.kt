package com.grippo.analytics.home

import com.grippo.analytics.home.models.HomeEvent
import com.grippo.analytics.home.models.HomeOperation
import com.grippo.analytics.home.models.HomeScreen
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation

public interface HomeAnalytics {
    public fun trackEvent(event: HomeEvent)

    public suspend fun <T> trackOperation(operation: HomeOperation, block: suspend () -> T): T

    public fun presentScreen(
        screen: HomeScreen,
        reason: NavigationReason = NavigationReason.Initial,
    ): ScreenPresentation
}
