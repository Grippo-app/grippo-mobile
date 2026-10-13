package com.grippo.analytics.period.picker.internal

import com.grippo.analytics.period.picker.PeriodPickerAnalytics
import com.grippo.analytics.period.picker.internal.schema.PeriodPickerEventMatrix
import com.grippo.analytics.period.picker.models.PeriodPickerEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [PeriodPickerAnalytics::class])
internal class PeriodPickerAnalyticsImpl(private val analytics: Analytics) : PeriodPickerAnalytics {
    override fun trackEvent(event: PeriodPickerEvent) {
        try {
            analytics.track(PeriodPickerEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block result delivery or dismissal.
        }
    }
}
