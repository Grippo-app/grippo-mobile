package com.grippo.analytics.home.internal

import com.grippo.analytics.home.HomeAnalytics
import com.grippo.analytics.home.internal.operation.HomeOperationEvents
import com.grippo.analytics.home.internal.schema.HomeEventMatrix
import com.grippo.analytics.home.models.HomeEvent
import com.grippo.analytics.home.models.HomeOperation
import com.grippo.analytics.home.models.HomeScreen
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.analytics.operation.trackOperation
import org.koin.core.annotation.Single

@Single(binds = [HomeAnalytics::class])
internal class HomeAnalyticsImpl(private val analytics: Analytics) : HomeAnalytics {
    override fun trackEvent(event: HomeEvent) {
        try {
            analytics.track(HomeEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block the observed user action.
        }
    }

    override suspend fun <T> trackOperation(operation: HomeOperation, block: suspend () -> T): T {
        return analytics.trackOperation(HomeOperationEvents<T>(operation), block = block)
    }

    override fun presentScreen(screen: HomeScreen, reason: NavigationReason): ScreenPresentation {
        val screenId = ScreenId(
            when (screen) {
                HomeScreen.Home -> "home"
            }
        )
        return analytics.presentScreen(NavigationContext(screen = screenId, reason = reason))
    }
}
