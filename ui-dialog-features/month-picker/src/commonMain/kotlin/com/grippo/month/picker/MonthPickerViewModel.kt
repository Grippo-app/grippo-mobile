package com.grippo.month.picker

import com.grippo.analytics.month.picker.MonthPickerAnalytics
import com.grippo.analytics.month.picker.models.MonthPickerEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.datetime.LocalDateTime

public class MonthPickerViewModel(
    initial: DateTimeFormatState,
    limitations: DateRange,
    private val analytics: MonthPickerAnalytics,
) : BaseViewModel<MonthPickerState, MonthPickerDirection, MonthPickerLoader>(
    MonthPickerState(
        value = initial,
        limitations = limitations,
    )
), MonthPickerContract {

    override fun onSelectMonth(value: LocalDateTime) {
        update { it.copy(value = DateTimeFormatState.of(value, it.limitations, it.value.format)) }
    }

    override fun onSubmitClick() {
        if (state.value.value !is DateTimeFormatState.Valid) return
        analytics.trackEvent(MonthPickerEvent.SubmitClicked)
        navigateTo(MonthPickerDirection.BackWithResult(state.value.value))
    }

    override fun onDismiss() {
        analytics.trackEvent(MonthPickerEvent.DismissRequested)
        navigateTo(MonthPickerDirection.Back)
    }
}
