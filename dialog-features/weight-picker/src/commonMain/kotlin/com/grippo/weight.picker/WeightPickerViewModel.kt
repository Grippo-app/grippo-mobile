package com.grippo.weight.picker

import com.grippo.core.BaseViewModel

public class WeightPickerViewModel(
    initial: Float
) : BaseViewModel<WeightPickerState, WeightPickerDirection, WeightPickerLoader>(
    WeightPickerState(value = initial)
), WeightPickerContract {

    override fun onSelectWeight(value: Float) {
        update { it.copy(value = value) }
    }

    override fun onSubmitClick() {
        navigateTo(WeightPickerDirection.BackWithResult(state.value.value))
    }

    override fun onDismiss() {
        navigateTo(WeightPickerDirection.Back)
    }
}