package com.grippo.date.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.date.utils.DateRange
import com.grippo.state.formatters.DateFormatState
import kotlinx.datetime.LocalDateTime

public class DatePickerViewModel(
    title: String,
    initial: DateFormatState,
    limitations: DateRange
) : BaseViewModel<DatePickerState, DatePickerDirection, DatePickerLoader>(
    DatePickerState(
        value = initial,
        limitations = limitations,
        title = title
    )
), DatePickerContract {

    override fun onSelectDate(value: LocalDateTime) {
        update { it.copy(value = DateFormatState.of(value, it.limitations)) }
    }

    override fun onSubmitClick() {
        navigateTo(DatePickerDirection.BackWithResult(state.value.value))
    }

    override fun onDismiss() {
        navigateTo(DatePickerDirection.Back)
    }
}