package com.grippo.analytics.month.picker.internal

import com.grippo.analytics.month.picker.MonthPickerAnalytics
import com.grippo.analytics.month.picker.internal.schema.MonthPickerEventMatrix
import com.grippo.analytics.month.picker.models.MonthPickerEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [MonthPickerAnalytics::class])
internal class MonthPickerAnalyticsImpl(private val analytics: Analytics) : MonthPickerAnalytics {
    override fun trackEvent(event: MonthPickerEvent) {
        try {
            analytics.track(MonthPickerEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block result delivery or dismissal.
        }
    }
}
