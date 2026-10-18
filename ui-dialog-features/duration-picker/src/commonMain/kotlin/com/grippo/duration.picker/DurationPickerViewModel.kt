package com.grippo.duration.picker

import com.grippo.analytics.duration.picker.DurationPickerAnalytics
import com.grippo.analytics.duration.picker.models.DurationPickerEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DurationFormatState
import kotlin.time.Duration

public class DurationPickerViewModel(
    initial: DurationFormatState,
    private val analytics: DurationPickerAnalytics,
) : BaseViewModel<DurationPickerState, DurationPickerDirection, DurationPickerLoader>(
    DurationPickerState(
        value = initial
    )
), DurationPickerContract {

    override fun onSelectDuration(value: Duration) {
        val duration = DurationFormatState.of(value)

        update { it.copy(value = duration) }
    }

    override fun onSubmitClick() {
        val duration = state.value.value as? DurationFormatState.Valid ?: return

        analytics.trackEvent(DurationPickerEvent.SubmitClicked)
        navigateTo(DurationPickerDirection.BackWithResult(duration))
    }

    override fun onDismiss() {
        analytics.trackEvent(DurationPickerEvent.DismissRequested)
        navigateTo(DurationPickerDirection.Back)
    }
}
