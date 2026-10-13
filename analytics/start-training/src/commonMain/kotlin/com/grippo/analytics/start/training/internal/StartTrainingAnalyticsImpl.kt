package com.grippo.analytics.start.training.internal

import com.grippo.analytics.start.training.StartTrainingAnalytics
import com.grippo.analytics.start.training.internal.operation.StartTrainingOperationEvents
import com.grippo.analytics.start.training.internal.schema.StartTrainingEventMatrix
import com.grippo.analytics.start.training.models.StartTrainingEvent
import com.grippo.analytics.start.training.models.StartTrainingOperation
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.operation.trackOperation
import org.koin.core.annotation.Single

@Single(binds = [StartTrainingAnalytics::class])
internal class StartTrainingAnalyticsImpl(private val analytics: Analytics) : StartTrainingAnalytics {
    override fun trackEvent(event: StartTrainingEvent) {
        try {
            analytics.track(StartTrainingEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block the observed user action.
        }
    }

    override suspend fun <T> trackOperation(operation: StartTrainingOperation, block: suspend () -> T): T {
        return analytics.trackOperation(StartTrainingOperationEvents<T>(operation), block = block)
    }
}
