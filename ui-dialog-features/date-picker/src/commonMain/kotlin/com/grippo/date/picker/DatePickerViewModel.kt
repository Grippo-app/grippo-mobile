package com.grippo.date.picker

import com.grippo.analytics.date.picker.DatePickerAnalytics
import com.grippo.analytics.date.picker.models.DatePickerEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.datetime.LocalDateTime

public class DatePickerViewModel(
    initial: DateTimeFormatState,
    limitations: DateRange,
    private val analytics: DatePickerAnalytics,
) : BaseViewModel<DatePickerState, DatePickerDirection, DatePickerLoader>(
    DatePickerState(
        value = initial,
        limitations = limitations,
    )
), DatePickerContract {

    override fun onSelectDate(value: LocalDateTime) {
        update { it.copy(value = DateTimeFormatState.of(value, it.limitations, it.value.format)) }
    }

    override fun onSubmitClick() {
        if (state.value.value !is DateTimeFormatState.Valid) return
        analytics.trackEvent(DatePickerEvent.SubmitClicked)
        navigateTo(DatePickerDirection.BackWithResult(state.value.value))
    }

    override fun onDismiss() {
        analytics.trackEvent(DatePickerEvent.DismissRequested)
        navigateTo(DatePickerDirection.Back)
    }
}
