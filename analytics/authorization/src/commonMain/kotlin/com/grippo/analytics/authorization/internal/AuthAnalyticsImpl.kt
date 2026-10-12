package com.grippo.analytics.authorization.internal

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.internal.operation.AuthOperationEvents
import com.grippo.analytics.authorization.internal.schema.AuthEventMatrix
import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.analytics.operation.trackOperation
import org.koin.core.annotation.Single

@Single(binds = [AuthAnalytics::class])
internal class AuthAnalyticsImpl(private val analytics: Analytics) : AuthAnalytics {
    override fun trackEvent(event: AuthEvent) {
        try {
            analytics.track(AuthEventMatrix.event(event))
        } catch (_: Exception) {
            // Invalid telemetry or a reporting failure cannot block a user action.
        }
    }

    override suspend fun <T> trackOperation(operation: AuthOperation, block: suspend () -> T): T {
        return analytics.trackOperation(AuthOperationEvents<T>(operation), block = block)
    }

    override fun presentScreen(screen: AuthScreen, reason: NavigationReason): ScreenPresentation {
        return analytics.presentScreen(
            NavigationContext(screen = ScreenId(screen.analyticsCode), reason = reason)
        )
    }
}
