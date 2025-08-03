package com.grippo.height.picker

import com.grippo.core.BaseViewModel

public class HeightPickerViewModel(
    initial: Int
) : BaseViewModel<HeightPickerState, HeightPickerDirection, HeightPickerLoader>(
    HeightPickerState(initial = initial)
), HeightPickerContract {

    override fun onSelectHeight(value: Int) {
        update { it.copy(initial = value) }
    }

    override fun onSubmitClick() {
        navigateTo(HeightPickerDirection.BackWithResult(state.value.initial))
    }

    override fun onDismiss() {
        navigateTo(HeightPickerDirection.Back)
    }
}