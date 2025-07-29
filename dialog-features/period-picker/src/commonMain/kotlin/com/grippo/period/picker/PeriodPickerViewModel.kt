package com.grippo.period.picker

import com.grippo.core.BaseViewModel
import com.grippo.state.datetime.PeriodState

public class PeriodPickerViewModel(
    initial: PeriodState
) : BaseViewModel<PeriodPickerState, PeriodPickerDirection, PeriodPickerLoader>(
    PeriodPickerState(initial = initial)
), PeriodPickerContract {

    override fun select(value: PeriodState) {
        update { it.copy(initial = value) }
    }

    override fun submit() {
        navigateTo(PeriodPickerDirection.BackWithResult(state.value.initial))
    }

    override fun dismiss() {
        navigateTo(PeriodPickerDirection.Back)
    }
}