package com.grippo.analytics.draft.training.internal

import com.grippo.analytics.draft.training.DraftTrainingAnalytics
import com.grippo.analytics.draft.training.internal.operation.DraftTrainingOperationEvents
import com.grippo.analytics.draft.training.internal.schema.DraftTrainingEventMatrix
import com.grippo.analytics.draft.training.models.DraftTrainingEvent
import com.grippo.analytics.draft.training.models.DraftTrainingOperation
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.operation.trackOperation
import org.koin.core.annotation.Single

@Single(binds = [DraftTrainingAnalytics::class])
internal class DraftTrainingAnalyticsImpl(private val analytics: Analytics) : DraftTrainingAnalytics {
    override fun trackEvent(event: DraftTrainingEvent) {
        try {
            analytics.track(DraftTrainingEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block the observed user action.
        }
    }

    override suspend fun <T> trackOperation(operation: DraftTrainingOperation, block: suspend () -> T): T {
        return analytics.trackOperation(DraftTrainingOperationEvents<T>(operation), block = block)
    }
}
