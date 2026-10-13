package com.grippo.analytics.exercises

import com.grippo.analytics.exercises.models.ExercisesEvent
import com.grippo.analytics.exercises.models.ExercisesScreen
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation

public interface ExercisesAnalytics {
    public fun trackEvent(event: ExercisesEvent)
    public fun presentScreen(
        screen: ExercisesScreen,
        reason: NavigationReason = NavigationReason.Initial,
    ): ScreenPresentation
}
