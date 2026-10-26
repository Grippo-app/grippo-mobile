package com.grippo.language.picker

import com.grippo.analytics.language.picker.LanguagePickerAnalytics
import com.grippo.analytics.language.picker.models.LanguagePickerEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.settings.LanguageEnumState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

public class LanguagePickerViewModel(
    initial: LanguageEnumState,
    private val analytics: LanguagePickerAnalytics,
) : BaseViewModel<LanguagePickerState, LanguagePickerDirection, LanguagePickerLoader>(
    LanguagePickerState(
        value = initial,
    )
), LanguagePickerContract {

    override fun onSelectLanguage(language: LanguageEnumState) {
        analytics.trackEvent(LanguagePickerEvent.LanguageClicked)
        safeLaunch {
            update { it.copy(value = language) }
            delay(300.milliseconds)
            navigateTo(LanguagePickerDirection.BackWithResult(language))
        }
    }

    override fun onDismiss() {
        analytics.trackEvent(LanguagePickerEvent.DismissRequested)
        navigateTo(LanguagePickerDirection.Back)
    }
}
