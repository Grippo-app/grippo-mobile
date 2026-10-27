package com.grippo.period.picker

import com.grippo.analytics.period.picker.PeriodPickerAnalytics
import com.grippo.analytics.period.picker.models.PeriodPickerEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

public class PeriodPickerViewModel(
    initial: DateRangeKind,
    private val analytics: PeriodPickerAnalytics,
) : BaseViewModel<PeriodPickerState, PeriodPickerDirection, PeriodPickerLoader>(
    PeriodPickerState(
        value = DateRangeFormatState.of(initial),
    )
), PeriodPickerContract {

    override fun onSelectRange(kind: DateRangeKind) {
        analytics.trackEvent(PeriodPickerEvent.RangeClicked)
        safeLaunch(recovery = ErrorRecovery.Retry) {
            update { it.copy(value = DateRangeFormatState.of(kind)) }
            delay(300.milliseconds)
            navigateTo(PeriodPickerDirection.BackWithResult(kind))
        }
    }

    override fun onDismiss() {
        analytics.trackEvent(PeriodPickerEvent.DismissRequested)
        navigateTo(PeriodPickerDirection.Back)
    }
}
