package com.grippo.analytics.primary.goal.picker.internal

import com.grippo.analytics.primary.goal.picker.PrimaryGoalPickerAnalytics
import com.grippo.analytics.primary.goal.picker.internal.schema.PrimaryGoalPickerEventMatrix
import com.grippo.analytics.primary.goal.picker.models.PrimaryGoalPickerEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [PrimaryGoalPickerAnalytics::class])
internal class PrimaryGoalPickerAnalyticsImpl(private val analytics: Analytics) : PrimaryGoalPickerAnalytics {
    override fun trackEvent(event: PrimaryGoalPickerEvent) {
        try {
            analytics.track(PrimaryGoalPickerEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block result delivery or dismissal.
        }
    }
}
