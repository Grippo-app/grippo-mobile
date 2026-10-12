package com.grippo.analytics.authorization

import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation

/** Stateless telemetry. Business results, navigation and error handling remain caller-owned. */
public interface AuthAnalytics {
    public fun trackEvent(event: AuthEvent)

    public suspend fun <T> trackOperation(operation: AuthOperation, block: suspend () -> T): T

    public fun presentScreen(
        screen: AuthScreen,
        reason: NavigationReason = NavigationReason.Initial,
    ): ScreenPresentation
}
