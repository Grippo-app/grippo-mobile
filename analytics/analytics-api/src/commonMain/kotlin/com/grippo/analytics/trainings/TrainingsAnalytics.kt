package com.grippo.analytics.trainings

import com.grippo.analytics.trainings.models.TrainingsEvent
import com.grippo.analytics.trainings.models.TrainingsOperation
import com.grippo.analytics.trainings.models.TrainingsScreen
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation

public interface TrainingsAnalytics {
    public fun trackEvent(event: TrainingsEvent)

    public suspend fun <T> trackOperation(operation: TrainingsOperation, block: suspend () -> T): T

    public fun presentScreen(
        screen: TrainingsScreen,
        reason: NavigationReason = NavigationReason.Initial,
    ): ScreenPresentation
}
