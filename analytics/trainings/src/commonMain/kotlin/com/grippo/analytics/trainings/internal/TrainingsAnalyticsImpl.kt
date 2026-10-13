package com.grippo.analytics.trainings.internal

import com.grippo.analytics.trainings.TrainingsAnalytics
import com.grippo.analytics.trainings.models.TrainingsScreen
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import org.koin.core.annotation.Single

@Single(binds = [TrainingsAnalytics::class])
internal class TrainingsAnalyticsImpl(private val analytics: Analytics) : TrainingsAnalytics {
    override fun presentScreen(screen: TrainingsScreen, reason: NavigationReason): ScreenPresentation {
        val screenId = ScreenId(
            when (screen) {
                TrainingsScreen.Calendar -> "trainings_calendar"
            }
        )
        return analytics.presentScreen(NavigationContext(screen = screenId, reason = reason))
    }
}
