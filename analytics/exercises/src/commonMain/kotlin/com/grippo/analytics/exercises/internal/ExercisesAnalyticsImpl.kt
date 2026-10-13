package com.grippo.analytics.exercises.internal

import com.grippo.analytics.exercises.ExercisesAnalytics
import com.grippo.analytics.exercises.internal.schema.ExercisesEventMatrix
import com.grippo.analytics.exercises.models.ExercisesEvent
import com.grippo.analytics.exercises.models.ExercisesScreen
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import org.koin.core.annotation.Single

@Single(binds = [ExercisesAnalytics::class])
internal class ExercisesAnalyticsImpl(private val analytics: Analytics) : ExercisesAnalytics {
    override fun trackEvent(event: ExercisesEvent) {
        try {
            analytics.track(ExercisesEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block the observed user action.
        }
    }

    override fun presentScreen(screen: ExercisesScreen, reason: NavigationReason): ScreenPresentation {
        val id = when (screen) {
            ExercisesScreen.Library -> "exercise_library"
        }
        return analytics.presentScreen(NavigationContext(screen = ScreenId(id), reason = reason))
    }
}
