package com.grippo.period.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.coroutines.delay

public class PeriodPickerViewModel(
    initial: DateRangeKind,
) : BaseViewModel<PeriodPickerState, PeriodPickerDirection, PeriodPickerLoader>(
    PeriodPickerState(
        value = DateRangeFormatState.of(initial),
    )
), PeriodPickerContract {

    override fun onSelectRange(kind: DateRangeKind) {
        safeLaunch {
            update { it.copy(value = DateRangeFormatState.of(kind)) }
            delay(300)
            navigateTo(PeriodPickerDirection.BackWithResult(kind))
        }
    }

    override fun onDismiss() {
        navigateTo(PeriodPickerDirection.Back)
    }
}
