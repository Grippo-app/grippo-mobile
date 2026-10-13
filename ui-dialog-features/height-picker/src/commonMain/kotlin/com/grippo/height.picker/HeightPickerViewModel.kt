package com.grippo.height.picker

import com.grippo.analytics.height.picker.HeightPickerAnalytics
import com.grippo.analytics.height.picker.models.HeightPickerEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.HeightFormatState

public class HeightPickerViewModel(
    initial: HeightFormatState,
    private val analytics: HeightPickerAnalytics,
) : BaseViewModel<HeightPickerState, HeightPickerDirection, HeightPickerLoader>(
    HeightPickerState(value = initial)
), HeightPickerContract {

    override fun onSelectHeight(value: Int) {
        update { it.copy(value = HeightFormatState.of(value)) }
    }

    override fun onSubmitClick() {
        analytics.trackEvent(HeightPickerEvent.SubmitClicked)
        navigateTo(HeightPickerDirection.BackWithResult(state.value.value))
    }

    override fun onDismiss() {
        analytics.trackEvent(HeightPickerEvent.DismissRequested)
        navigateTo(HeightPickerDirection.Back)
    }
}
