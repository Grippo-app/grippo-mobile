package com.grippo.weight.picker

import com.grippo.core.BaseViewModel

public class WeightPickerViewModel(
    initial: Float
) : BaseViewModel<WeightPickerState, WeightPickerDirection, WeightPickerLoader>(
    WeightPickerState(initial = initial)
), WeightPickerContract {

    override fun onSelectWeight(value: Float) {
        update { it.copy(initial = value) }
    }

    override fun onSubmitClick() {
        navigateTo(WeightPickerDirection.BackWithResult(state.value.initial))
    }

    override fun onDismiss() {
        navigateTo(WeightPickerDirection.Back)
    }
}