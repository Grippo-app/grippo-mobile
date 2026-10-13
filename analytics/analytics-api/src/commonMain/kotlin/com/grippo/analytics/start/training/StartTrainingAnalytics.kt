package com.grippo.analytics.start.training

import com.grippo.analytics.start.training.models.StartTrainingEvent
import com.grippo.analytics.start.training.models.StartTrainingOperation

public interface StartTrainingAnalytics {
    public fun trackEvent(event: StartTrainingEvent)
    public suspend fun <T> trackOperation(operation: StartTrainingOperation, block: suspend () -> T): T
}
