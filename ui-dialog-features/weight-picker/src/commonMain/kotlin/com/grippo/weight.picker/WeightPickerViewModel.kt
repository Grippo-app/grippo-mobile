package com.grippo.weight.picker

import com.grippo.core.BaseViewModel
import com.grippo.state.formatters.WeightFormatState

public class WeightPickerViewModel(
    initial: WeightFormatState
) : BaseViewModel<WeightPickerState, WeightPickerDirection, WeightPickerLoader>(
    WeightPickerState(value = initial)
), WeightPickerContract {

    override fun onSelectWeight(value: Float) {
        update { it.copy(value = WeightFormatState.of(value)) }
    }

    override fun onSubmitClick() {
        navigateTo(WeightPickerDirection.BackWithResult(state.value.value))
    }

    override fun onDismiss() {
        navigateTo(WeightPickerDirection.Back)
    }
}