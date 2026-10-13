package com.grippo.analytics.training.streak.details.internal

import com.grippo.analytics.training.streak.details.TrainingStreakDetailsAnalytics
import com.grippo.analytics.training.streak.details.internal.schema.TrainingStreakDetailsEventMatrix
import com.grippo.analytics.training.streak.details.models.TrainingStreakDetailsEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [TrainingStreakDetailsAnalytics::class])
internal class TrainingStreakDetailsAnalyticsImpl(private val analytics: Analytics) : TrainingStreakDetailsAnalytics {
    override fun trackEvent(event: TrainingStreakDetailsEvent) {
        try {
            analytics.track(TrainingStreakDetailsEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block result delivery or dismissal.
        }
    }
}
