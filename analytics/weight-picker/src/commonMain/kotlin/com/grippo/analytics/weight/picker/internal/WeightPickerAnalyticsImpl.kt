package com.grippo.analytics.weight.picker.internal

import com.grippo.analytics.weight.picker.WeightPickerAnalytics
import com.grippo.analytics.weight.picker.internal.schema.WeightPickerEventMatrix
import com.grippo.analytics.weight.picker.models.WeightPickerEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [WeightPickerAnalytics::class])
internal class WeightPickerAnalyticsImpl(private val analytics: Analytics) : WeightPickerAnalytics {
    override fun trackEvent(event: WeightPickerEvent) {
        try {
            analytics.track(WeightPickerEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block result delivery or dismissal.
        }
    }
}
