package com.grippo.height.picker

import com.grippo.core.BaseViewModel

public class HeightPickerViewModel(
    initial: Int
) : BaseViewModel<HeightPickerState, HeightPickerDirection, HeightPickerLoader>(
    HeightPickerState(value = initial)
), HeightPickerContract {

    override fun onSelectHeight(value: Int) {
        update { it.copy(value = value) }
    }

    override fun onSubmitClick() {
        navigateTo(HeightPickerDirection.BackWithResult(state.value.value))
    }

    override fun onDismiss() {
        navigateTo(HeightPickerDirection.Back)
    }
}