package com.grippo.duration.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DurationFormatState
import kotlin.time.Duration

public class DurationPickerViewModel(
    initial: DurationFormatState
) : BaseViewModel<DurationPickerState, DurationPickerDirection, DurationPickerLoader>(
    DurationPickerState(
        value = initial
    )
), DurationPickerContract {

    override fun onSelectDuration(value: Duration) {
        update { it.copy(value = DurationFormatState.of(value)) }
    }

    override fun onSubmitClick() {
        navigateTo(DurationPickerDirection.BackWithResult(state.value.value))
    }

    override fun onDismiss() {
        navigateTo(DurationPickerDirection.Back)
    }
}
