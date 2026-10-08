package com.grippo.confirm.training.completion

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DurationFormatState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

public class ConfirmTrainingCompletionViewModel(
    initial: DurationFormatState,
) : BaseViewModel<ConfirmTrainingCompletionState, ConfirmTrainingCompletionDirection, ConfirmTrainingCompletionLoader>(
    ConfirmTrainingCompletionState(
        duration = DurationFormatState.of(
            (initial.value ?: Duration.ZERO).coerceAtLeast(1.minutes)
        )
    )
), ConfirmTrainingCompletionContract {

    override fun onConfirm() {
        navigateTo(ConfirmTrainingCompletionDirection.Confirm(state.value.duration))
    }

    override fun onDurationInputClick() {
        navigateTo(ConfirmTrainingCompletionDirection.PickDuration(state.value.duration.value))
    }

    public fun onDurationSelected(value: Duration) {
        update { it.copy(duration = DurationFormatState.of(value)) }
    }

    override fun onBack() {
        navigateTo(ConfirmTrainingCompletionDirection.Back)
    }
}
