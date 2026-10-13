package com.grippo.analytics.secondary.goal.picker.internal

import com.grippo.analytics.secondary.goal.picker.SecondaryGoalPickerAnalytics
import com.grippo.analytics.secondary.goal.picker.internal.schema.SecondaryGoalPickerEventMatrix
import com.grippo.analytics.secondary.goal.picker.models.SecondaryGoalPickerEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [SecondaryGoalPickerAnalytics::class])
internal class SecondaryGoalPickerAnalyticsImpl(private val analytics: Analytics) : SecondaryGoalPickerAnalytics {
    override fun trackEvent(event: SecondaryGoalPickerEvent) {
        try {
            analytics.track(SecondaryGoalPickerEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block result delivery or dismissal.
        }
    }
}
