package com.grippo.analytics.debug.internal

import com.grippo.analytics.debug.DebugAnalytics
import com.grippo.analytics.debug.models.DebugScreen
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import org.koin.core.annotation.Single

@Single(binds = [DebugAnalytics::class])
internal class DebugAnalyticsImpl(private val analytics: Analytics) : DebugAnalytics {
    override fun presentScreen(screen: DebugScreen, reason: NavigationReason): ScreenPresentation {
        val id = when (screen) {
            DebugScreen.Debug -> "debug"
        }
        return analytics.presentScreen(NavigationContext(screen = ScreenId(id), reason = reason))
    }
}
