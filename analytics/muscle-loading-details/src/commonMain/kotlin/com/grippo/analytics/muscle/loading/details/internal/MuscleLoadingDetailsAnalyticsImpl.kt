package com.grippo.analytics.muscle.loading.details.internal

import com.grippo.analytics.muscle.loading.details.MuscleLoadingDetailsAnalytics
import com.grippo.analytics.muscle.loading.details.internal.schema.MuscleLoadingDetailsEventMatrix
import com.grippo.analytics.muscle.loading.details.models.MuscleLoadingDetailsEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [MuscleLoadingDetailsAnalytics::class])
internal class MuscleLoadingDetailsAnalyticsImpl(
    private val analytics: Analytics,
) : MuscleLoadingDetailsAnalytics {
    override fun trackEvent(event: MuscleLoadingDetailsEvent) {
        try {
            analytics.track(MuscleLoadingDetailsEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block selection or dismissal.
        }
    }
}
