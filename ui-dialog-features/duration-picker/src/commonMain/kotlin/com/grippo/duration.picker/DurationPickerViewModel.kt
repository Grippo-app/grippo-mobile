package com.grippo.duration.picker

import com.grippo.core.foundation.BaseViewModel
import kotlin.time.Duration

public class DurationPickerViewModel(
    initial: Duration
) : BaseViewModel<DurationPickerState, DurationPickerDirection, DurationPickerLoader>(
    DurationPickerState(
        hours = resolveDurationHours(initial),
        value = initial
    )
), DurationPickerContract {

    override fun onSelectDuration(value: Duration) {
        update { it.copy(value = value) }
    }

    override fun onSubmitClick() {
        navigateTo(DurationPickerDirection.BackWithResult(state.value.value))
    }

    override fun onDismiss() {
        navigateTo(DurationPickerDirection.Back)
    }
}
