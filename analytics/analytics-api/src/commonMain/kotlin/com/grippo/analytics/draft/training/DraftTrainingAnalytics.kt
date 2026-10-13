package com.grippo.analytics.draft.training

import com.grippo.analytics.draft.training.models.DraftTrainingEvent
import com.grippo.analytics.draft.training.models.DraftTrainingOperation

public interface DraftTrainingAnalytics {
    public fun trackEvent(event: DraftTrainingEvent)
    public suspend fun <T> trackOperation(operation: DraftTrainingOperation, block: suspend () -> T): T
}
