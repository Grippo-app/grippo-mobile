package com.grippo.analytics.training

import com.grippo.analytics.training.models.TrainingScreen
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation

public interface TrainingAnalytics {
    public fun presentScreen(
        screen: TrainingScreen,
        reason: NavigationReason = NavigationReason.Initial,
    ): ScreenPresentation
}
