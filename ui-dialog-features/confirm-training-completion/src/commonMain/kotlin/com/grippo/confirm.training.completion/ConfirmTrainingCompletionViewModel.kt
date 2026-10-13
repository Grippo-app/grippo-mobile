package com.grippo.confirm.training.completion

import com.grippo.analytics.training.TrainingAnalytics
import com.grippo.analytics.training.models.TrainingEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DurationFormatState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

public class ConfirmTrainingCompletionViewModel(
    initial: DurationFormatState,
    private val analytics: TrainingAnalytics,
) : BaseViewModel<ConfirmTrainingCompletionState, ConfirmTrainingCompletionDirection, ConfirmTrainingCompletionLoader>(
    ConfirmTrainingCompletionState(
        duration = DurationFormatState.of(
            (initial.value ?: Duration.ZERO).coerceAtLeast(1.minutes)
        )
    )
), ConfirmTrainingCompletionContract {

    override fun onConfirm() {
        analytics.trackEvent(TrainingEvent.CompletionConfirmation.Confirm)
        navigateTo(ConfirmTrainingCompletionDirection.Confirm(state.value.duration))
    }

    override fun onDurationInputClick() {
        analytics.trackEvent(TrainingEvent.CompletionConfirmation.DurationOpened)
        navigateTo(ConfirmTrainingCompletionDirection.PickDuration(state.value.duration.value))
    }

    public fun onDurationSelected(value: Duration) {
        analytics.trackEvent(TrainingEvent.CompletionConfirmation.DurationSelected)
        update { it.copy(duration = DurationFormatState.of(value)) }
    }

    override fun onBack() {
        analytics.trackEvent(TrainingEvent.CompletionConfirmation.Back)
        navigateTo(ConfirmTrainingCompletionDirection.Back)
    }
}
