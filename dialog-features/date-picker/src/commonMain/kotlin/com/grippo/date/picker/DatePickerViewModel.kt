package com.grippo.date.picker

import com.grippo.core.BaseViewModel
import com.grippo.date.utils.DateRange
import com.grippo.state.formatters.DateFormatState
import kotlinx.datetime.LocalDateTime

public class DatePickerViewModel(
    initial: LocalDateTime,
    limitations: DateRange
) : BaseViewModel<DatePickerState, DatePickerDirection, DatePickerLoader>(
    DatePickerState(
        value = DateFormatState.of(initial, limitations),
        limitations = limitations
    )
), DatePickerContract {

    override fun onSelectDate(value: LocalDateTime) {
        update { it.copy(value = DateFormatState.of(value, it.limitations)) }
    }

    override fun onSubmitClick() {
        navigateTo(DatePickerDirection.BackWithResult(state.value.value.value))
    }

    override fun onDismiss() {
        navigateTo(DatePickerDirection.Back)
    }
}