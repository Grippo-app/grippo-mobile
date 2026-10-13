package com.grippo.analytics.training.goal.details.internal

import com.grippo.analytics.training.goal.details.TrainingGoalDetailsAnalytics
import com.grippo.analytics.training.goal.details.internal.schema.TrainingGoalDetailsEventMatrix
import com.grippo.analytics.training.goal.details.models.TrainingGoalDetailsEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [TrainingGoalDetailsAnalytics::class])
internal class TrainingGoalDetailsAnalyticsImpl(private val analytics: Analytics) : TrainingGoalDetailsAnalytics {
    override fun trackEvent(event: TrainingGoalDetailsEvent) {
        try {
            analytics.track(TrainingGoalDetailsEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block result delivery or dismissal.
        }
    }
}
