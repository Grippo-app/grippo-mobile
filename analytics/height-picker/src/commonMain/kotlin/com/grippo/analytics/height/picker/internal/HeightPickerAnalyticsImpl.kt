package com.grippo.analytics.height.picker.internal

import com.grippo.analytics.height.picker.HeightPickerAnalytics
import com.grippo.analytics.height.picker.internal.schema.HeightPickerEventMatrix
import com.grippo.analytics.height.picker.models.HeightPickerEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [HeightPickerAnalytics::class])
internal class HeightPickerAnalyticsImpl(private val analytics: Analytics) : HeightPickerAnalytics {
    override fun trackEvent(event: HeightPickerEvent) {
        try {
            analytics.track(HeightPickerEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block result delivery or dismissal.
        }
    }
}
