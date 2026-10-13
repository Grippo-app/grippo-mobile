package com.grippo.analytics.training.internal

import com.grippo.analytics.training.TrainingAnalytics
import com.grippo.analytics.training.models.TrainingScreen
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import org.koin.core.annotation.Single

@Single(binds = [TrainingAnalytics::class])
internal class TrainingAnalyticsImpl(private val analytics: Analytics) : TrainingAnalytics {
    override fun presentScreen(screen: TrainingScreen, reason: NavigationReason): ScreenPresentation {
        val screenId = ScreenId(
            when (screen) {
                TrainingScreen.Recording -> "training_recording"
                TrainingScreen.Exercise -> "training_exercise"
            }
        )
        return analytics.presentScreen(NavigationContext(screen = screenId, reason = reason))
    }
}
