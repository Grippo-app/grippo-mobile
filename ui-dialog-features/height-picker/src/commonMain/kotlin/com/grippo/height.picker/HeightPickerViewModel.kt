package com.grippo.height.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.state.formatters.HeightFormatState

public class HeightPickerViewModel(
    initial: HeightFormatState
) : BaseViewModel<HeightPickerState, HeightPickerDirection, HeightPickerLoader>(
    HeightPickerState(value = initial)
), HeightPickerContract {

    override fun onSelectHeight(value: Int) {
        update { it.copy(value = HeightFormatState.of(value)) }
    }

    override fun onSubmitClick() {
        navigateTo(HeightPickerDirection.BackWithResult(state.value.value))
    }

    override fun onDismiss() {
        navigateTo(HeightPickerDirection.Back)
    }
}