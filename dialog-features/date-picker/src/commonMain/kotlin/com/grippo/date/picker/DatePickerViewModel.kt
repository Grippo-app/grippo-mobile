package com.grippo.date.picker

import com.grippo.core.BaseViewModel
import kotlinx.datetime.LocalDateTime

public class DatePickerViewModel(
    initial: LocalDateTime
) : BaseViewModel<DatePickerState, DatePickerDirection, DatePickerLoader>(
    DatePickerState(initial = initial)
), DatePickerContract {

    override fun select(value: LocalDateTime) {
        update { it.copy(initial = value) }
    }

    override fun submit() {
        navigateTo(DatePickerDirection.BackWithResult(state.value.initial))
    }

    override fun dismiss() {
        navigateTo(DatePickerDirection.Back)
    }
}