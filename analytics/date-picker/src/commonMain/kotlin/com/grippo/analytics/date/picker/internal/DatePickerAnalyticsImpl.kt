package com.grippo.analytics.date.picker.internal

import com.grippo.analytics.date.picker.DatePickerAnalytics
import com.grippo.analytics.date.picker.internal.schema.DatePickerEventMatrix
import com.grippo.analytics.date.picker.models.DatePickerEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [DatePickerAnalytics::class])
internal class DatePickerAnalyticsImpl(private val analytics: Analytics) : DatePickerAnalytics {
    override fun trackEvent(event: DatePickerEvent) {
        try {
            analytics.track(DatePickerEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block the observed user action.
        }
    }
}
