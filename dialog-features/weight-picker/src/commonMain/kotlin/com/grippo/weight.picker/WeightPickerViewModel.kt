package com.grippo.weight.picker

import com.grippo.core.BaseViewModel

public class WeightPickerViewModel(
    initial: Float
) : BaseViewModel<WeightPickerState, WeightPickerDirection, WeightPickerLoader>(
    WeightPickerState(initial = initial)
), WeightPickerContract {

    override fun select(value: Float) {
        update { it.copy(initial = value) }
    }

    override fun submit() {
        navigateTo(WeightPickerDirection.DismissWithResult(state.value.initial))
    }

    override fun dismiss() {
        navigateTo(WeightPickerDirection.Dismiss)
    }
}