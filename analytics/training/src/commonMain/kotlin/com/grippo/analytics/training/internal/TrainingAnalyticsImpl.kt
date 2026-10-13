package com.grippo.analytics.training.internal

import com.grippo.analytics.training.TrainingAnalytics
import com.grippo.analytics.training.internal.operation.TrainingOperationEvents
import com.grippo.analytics.training.internal.schema.TrainingEventMatrix
import com.grippo.analytics.training.models.TrainingEvent
import com.grippo.analytics.training.models.TrainingOperation
import com.grippo.analytics.training.models.TrainingScreen
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.analytics.operation.trackOperation
import org.koin.core.annotation.Single

@Single(binds = [TrainingAnalytics::class])
internal class TrainingAnalyticsImpl(private val analytics: Analytics) : TrainingAnalytics {
    override fun trackEvent(event: TrainingEvent) {
        try {
            analytics.track(TrainingEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block the observed user action.
        }
    }

    override suspend fun <T> trackOperation(operation: TrainingOperation, block: suspend () -> T): T {
        return analytics.trackOperation(TrainingOperationEvents<T>(operation), block = block)
    }

    override fun presentScreen(screen: TrainingScreen, reason: NavigationReason): ScreenPresentation {
        val screenId = ScreenId(
            when (screen) {
                TrainingScreen.Recording -> "training_recording"
                TrainingScreen.Exercise -> "training_exercise"
                TrainingScreen.Completed -> "training_completed"
            }
        )
        return analytics.presentScreen(NavigationContext(screen = screenId, reason = reason))
    }
}
