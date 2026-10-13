package com.grippo.analytics.profile.internal

import com.grippo.analytics.profile.ProfileAnalytics
import com.grippo.analytics.profile.internal.encoding.ProfileEncoding
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import org.koin.core.annotation.Single

@Single(binds = [ProfileAnalytics::class])
internal class ProfileAnalyticsImpl(private val analytics: Analytics) : ProfileAnalytics {
    override fun presentScreen(screen: ProfileScreen, reason: NavigationReason): ScreenPresentation {
        return analytics.presentScreen(
            NavigationContext(screen = ProfileEncoding.screen(screen), reason = reason)
        )
    }
}
