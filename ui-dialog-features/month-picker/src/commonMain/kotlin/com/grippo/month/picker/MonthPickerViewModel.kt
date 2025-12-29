package com.grippo.month.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.datetime.LocalDateTime

public class MonthPickerViewModel(
    title: String,
    initial: DateFormatState,
    limitations: DateRange
) : BaseViewModel<MonthPickerState, MonthPickerDirection, MonthPickerLoader>(
    MonthPickerState(
        value = initial,
        limitations = limitations,
        title = title
    )
), MonthPickerContract {

    override fun onSelectMonth(value: LocalDateTime) {
        update { it.copy(value = DateFormatState.of(value, it.limitations)) }
    }

    override fun onSubmitClick() {
        navigateTo(MonthPickerDirection.BackWithResult(state.value.value))
    }

    override fun onDismiss() {
        navigateTo(MonthPickerDirection.Back)
    }
}
