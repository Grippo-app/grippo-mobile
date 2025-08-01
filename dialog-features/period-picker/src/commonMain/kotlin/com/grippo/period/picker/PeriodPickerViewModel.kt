package com.grippo.period.picker

import com.grippo.core.BaseViewModel
import com.grippo.state.datetime.PeriodState
import kotlinx.collections.immutable.toPersistentList

public class PeriodPickerViewModel(
    initial: PeriodState,
    available: List<PeriodState>
) : BaseViewModel<PeriodPickerState, PeriodPickerDirection, PeriodPickerLoader>(
    PeriodPickerState(
        initial = initial,
        list = available.toPersistentList()
    )
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