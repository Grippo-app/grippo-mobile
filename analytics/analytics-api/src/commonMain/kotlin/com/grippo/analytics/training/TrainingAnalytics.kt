package com.grippo.analytics.training

import com.grippo.analytics.training.models.TrainingEvent
import com.grippo.analytics.training.models.TrainingOperation
import com.grippo.analytics.training.models.TrainingScreen
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation

public interface TrainingAnalytics {
    public fun trackEvent(event: TrainingEvent)

    public suspend fun <T> trackOperation(operation: TrainingOperation, block: suspend () -> T): T

    public fun presentScreen(
        screen: TrainingScreen,
        reason: NavigationReason = NavigationReason.Initial,
    ): ScreenPresentation
}
