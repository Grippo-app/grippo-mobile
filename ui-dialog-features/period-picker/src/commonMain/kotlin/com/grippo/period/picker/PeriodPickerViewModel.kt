package com.grippo.period.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.PeriodFormatState
import com.grippo.toolkit.date.utils.DateRange

public class PeriodPickerViewModel(
    title: String,
    initial: PeriodFormatState,
) : BaseViewModel<PeriodPickerState, PeriodPickerDirection, PeriodPickerLoader>(
    PeriodPickerState(
        value = initial,
        title = title
    )
), PeriodPickerContract {

    override fun onSelectRange(range: DateRange) {
        navigateTo(PeriodPickerDirection.BackWithResult(PeriodFormatState.of(range)))
    }

    override fun onDismiss() {
        navigateTo(PeriodPickerDirection.Back)
    }
}
