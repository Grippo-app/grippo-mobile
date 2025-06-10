package com.grippo.height.picker

import com.grippo.core.BaseViewModel

public class HeightPickerViewModel(
    initial: Int
) : BaseViewModel<HeightPickerState, HeightPickerDirection, HeightPickerLoader>(
    HeightPickerState(initial = initial)
), HeightPickerContract {

    override fun select(value: Int) {
        update { it.copy(initial = value) }
    }

    override fun submit() {
        navigateTo(HeightPickerDirection.BackWithResult(state.value.initial))
    }

    override fun dismiss() {
        navigateTo(HeightPickerDirection.Back)
    }
}