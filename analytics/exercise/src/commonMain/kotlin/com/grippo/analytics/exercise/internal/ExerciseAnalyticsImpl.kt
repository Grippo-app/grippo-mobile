package com.grippo.analytics.exercise.internal

import com.grippo.analytics.exercise.ExerciseAnalytics
import com.grippo.analytics.exercise.internal.schema.ExerciseEventMatrix
import com.grippo.analytics.exercise.models.ExerciseEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [ExerciseAnalytics::class])
internal class ExerciseAnalyticsImpl(private val analytics: Analytics) : ExerciseAnalytics {
    override fun trackEvent(event: ExerciseEvent) {
        try {
            analytics.track(ExerciseEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block detail navigation or dismissal.
        }
    }
}
