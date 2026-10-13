package com.grippo.analytics.iteration.picker.internal

import com.grippo.analytics.iteration.picker.IterationPickerAnalytics
import com.grippo.analytics.iteration.picker.internal.schema.IterationPickerEventMatrix
import com.grippo.analytics.iteration.picker.models.IterationPickerEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [IterationPickerAnalytics::class])
internal class IterationPickerAnalyticsImpl(private val analytics: Analytics) : IterationPickerAnalytics {
    override fun trackEvent(event: IterationPickerEvent) {
        try {
            analytics.track(IterationPickerEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block result delivery or dismissal.
        }
    }
}
