package com.grippo.weight.picker

import com.grippo.analytics.weight.picker.WeightPickerAnalytics
import com.grippo.analytics.weight.picker.models.WeightPickerEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.WeightFormatState

public class WeightPickerViewModel(
    initial: WeightFormatState,
    private val analytics: WeightPickerAnalytics
) : BaseViewModel<WeightPickerState, WeightPickerDirection, WeightPickerLoader>(
    WeightPickerState(value = initial)
), WeightPickerContract {

    override fun onSelectWeight(value: Float) {
        val selected = WeightFormatState.of(value)
        if (selected != state.value.value) {
            analytics.trackEvent(WeightPickerEvent.SelectionChanged)
        }
        update { it.copy(value = selected) }
    }

    override fun onSubmitClick() {
        analytics.trackEvent(WeightPickerEvent.SubmitClicked)
        navigateTo(WeightPickerDirection.BackWithResult(state.value.value))
    }

    override fun onDismiss() {
        analytics.trackEvent(WeightPickerEvent.DismissRequested)
        navigateTo(WeightPickerDirection.Back)
    }
}
