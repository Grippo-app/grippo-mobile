package com.grippo.date.picker

import com.grippo.core.BaseViewModel
import com.grippo.date.utils.DateRange
import kotlinx.datetime.LocalDateTime

public class DatePickerViewModel(
    initial: LocalDateTime,
    limitations: DateRange
) : BaseViewModel<DatePickerState, DatePickerDirection, DatePickerLoader>(
    DatePickerState(initial = initial, limitations = limitations)
), DatePickerContract {

    override fun onSelectDate(value: LocalDateTime) {
        update { it.copy(initial = value) }
    }

    override fun onSubmitClick() {
        navigateTo(DatePickerDirection.BackWithResult(state.value.initial))
    }

    override fun onDismiss() {
        navigateTo(DatePickerDirection.Back)
    }
}