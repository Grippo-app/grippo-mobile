package com.grippo.period.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.toolkit.date.utils.DateRange

public class PeriodPickerViewModel(
    title: String,
    initial: DateRange.Range,
) : BaseViewModel<PeriodPickerState, PeriodPickerDirection, PeriodPickerLoader>(
    PeriodPickerState(
        value = initial,
        title = title
    )
), PeriodPickerContract {

    override fun onSelectRange(range: DateRange.Range) {
        navigateTo(PeriodPickerDirection.BackWithResult(range))
    }

    override fun onDismiss() {
        navigateTo(PeriodPickerDirection.Back)
    }
}
