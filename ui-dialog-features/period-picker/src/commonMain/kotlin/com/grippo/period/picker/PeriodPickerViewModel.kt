package com.grippo.period.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.toolkit.date.utils.DateRangeKind

public class PeriodPickerViewModel(
    title: String,
    initial: DateRangeKind,
) : BaseViewModel<PeriodPickerState, PeriodPickerDirection, PeriodPickerLoader>(
    PeriodPickerState(
        value = DateRangeFormatState.ofPreset(initial),
        title = title
    )
), PeriodPickerContract {

    override fun onSelectRange(kind: DateRangeKind) {
        navigateTo(PeriodPickerDirection.BackWithResult(kind))
    }

    override fun onDismiss() {
        navigateTo(PeriodPickerDirection.Back)
    }
}
