package com.grippo.analytics.home.internal

import com.grippo.analytics.home.HomeAnalytics
import com.grippo.analytics.home.models.HomeScreen
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import org.koin.core.annotation.Single

@Single(binds = [HomeAnalytics::class])
internal class HomeAnalyticsImpl(private val analytics: Analytics) : HomeAnalytics {
    override fun presentScreen(screen: HomeScreen, reason: NavigationReason): ScreenPresentation {
        val screenId = ScreenId(
            when (screen) {
                HomeScreen.Home -> "home"
            }
        )
        return analytics.presentScreen(NavigationContext(screen = screenId, reason = reason))
    }
}
