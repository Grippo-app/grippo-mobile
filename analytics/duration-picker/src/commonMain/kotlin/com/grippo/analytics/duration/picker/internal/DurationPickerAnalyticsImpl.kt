package com.grippo.analytics.duration.picker.internal

import com.grippo.analytics.duration.picker.DurationPickerAnalytics
import com.grippo.analytics.duration.picker.internal.schema.DurationPickerEventMatrix
import com.grippo.analytics.duration.picker.models.DurationPickerEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [DurationPickerAnalytics::class])
internal class DurationPickerAnalyticsImpl(private val analytics: Analytics) : DurationPickerAnalytics {
    override fun trackEvent(event: DurationPickerEvent) {
        try {
            analytics.track(DurationPickerEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block result delivery or dismissal.
        }
    }
}
