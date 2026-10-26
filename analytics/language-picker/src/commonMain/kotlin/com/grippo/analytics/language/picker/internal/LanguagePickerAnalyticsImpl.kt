package com.grippo.analytics.language.picker.internal

import com.grippo.analytics.language.picker.LanguagePickerAnalytics
import com.grippo.analytics.language.picker.internal.schema.LanguagePickerEventMatrix
import com.grippo.analytics.language.picker.models.LanguagePickerEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [LanguagePickerAnalytics::class])
internal class LanguagePickerAnalyticsImpl(private val analytics: Analytics) : LanguagePickerAnalytics {
    override fun trackEvent(event: LanguagePickerEvent) {
        try {
            analytics.track(LanguagePickerEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block result delivery or dismissal.
        }
    }
}
