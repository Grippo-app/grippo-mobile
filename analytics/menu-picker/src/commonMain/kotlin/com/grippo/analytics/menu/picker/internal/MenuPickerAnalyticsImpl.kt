package com.grippo.analytics.menu.picker.internal

import com.grippo.analytics.menu.picker.MenuPickerAnalytics
import com.grippo.analytics.menu.picker.internal.schema.MenuPickerEventMatrix
import com.grippo.analytics.menu.picker.models.MenuPickerEvent
import com.grippo.toolkit.analytics.Analytics
import org.koin.core.annotation.Single

@Single(binds = [MenuPickerAnalytics::class])
internal class MenuPickerAnalyticsImpl(private val analytics: Analytics) : MenuPickerAnalytics {
    override fun trackEvent(event: MenuPickerEvent) {
        try {
            analytics.track(MenuPickerEventMatrix.event(event))
        } catch (_: Exception) {
            // Reporting cannot block result delivery or dismissal.
        }
    }
}
