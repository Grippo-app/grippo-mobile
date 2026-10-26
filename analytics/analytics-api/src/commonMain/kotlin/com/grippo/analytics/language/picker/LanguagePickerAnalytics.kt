package com.grippo.analytics.language.picker

import com.grippo.analytics.language.picker.models.LanguagePickerEvent

public interface LanguagePickerAnalytics {
    public fun trackEvent(event: LanguagePickerEvent)
}
