package com.grippo.analytics.trainings.internal

import com.grippo.analytics.trainings.TrainingsAnalytics
import com.grippo.analytics.trainings.internal.operation.TrainingsOperationEvents
import com.grippo.analytics.trainings.internal.schema.TrainingsEventMatrix
import com.grippo.analytics.trainings.models.TrainingsEvent
import com.grippo.analytics.trainings.models.TrainingsOperation
import com.grippo.analytics.trainings.models.TrainingsScreen
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.analytics.operation.trackOperation
import org.koin.core.annotation.Single

@Single(binds = [TrainingsAnalytics::class])
internal class TrainingsAnalyticsImpl(private val analytics: Analytics) : TrainingsAnalytics {
    override fun trackEvent(event: TrainingsEvent) {
        try {
            analytics.track(TrainingsEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block the observed user action.
        }
    }

    override suspend fun <T> trackOperation(operation: TrainingsOperation, block: suspend () -> T): T {
        return analytics.trackOperation(TrainingsOperationEvents<T>(operation), block = block)
    }

    override fun presentScreen(screen: TrainingsScreen, reason: NavigationReason): ScreenPresentation {
        val screenId = ScreenId(
            when (screen) {
                TrainingsScreen.Calendar -> "trainings_calendar"
            }
        )
        return analytics.presentScreen(NavigationContext(screen = screenId, reason = reason))
    }
}
