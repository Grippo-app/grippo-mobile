package com.grippo.confirm.training.completion

import com.grippo.analytics.training.TrainingAnalytics
import com.grippo.analytics.training.models.TrainingEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.trainings.ExerciseState
import kotlin.time.Duration
import kotlinx.collections.immutable.ImmutableList

public class ConfirmTrainingCompletionViewModel(
    initial: DurationFormatState,
    exercises: ImmutableList<ExerciseState>,
    private val analytics: TrainingAnalytics,
) : BaseViewModel<ConfirmTrainingCompletionState, ConfirmTrainingCompletionDirection, ConfirmTrainingCompletionLoader>(
    ConfirmTrainingCompletionState(
        duration = DurationFormatState.of(
            initial.value
                ?.coerceAtLeast(DurationFormatState.DurationLimitation.start)
                ?: DurationFormatState.DurationLimitation.start
        ),
        exercises = exercises,
    )
), ConfirmTrainingCompletionContract {

    override fun onConfirm() {
        val duration = state.value.duration as? DurationFormatState.Valid ?: return

        analytics.trackEvent(TrainingEvent.CompletionConfirmation.ConfirmClicked)
        navigateTo(ConfirmTrainingCompletionDirection.Confirm(duration))
    }

    override fun onDurationInputClick() {
        analytics.trackEvent(TrainingEvent.CompletionConfirmation.DurationOpened)
        navigateTo(ConfirmTrainingCompletionDirection.PickDuration(state.value.duration.value))
    }

    public fun onDurationSelected(value: Duration) {
        analytics.trackEvent(TrainingEvent.CompletionConfirmation.DurationSelected)

        val duration = DurationFormatState.of(value)

        update { it.copy(duration = duration) }
    }

    override fun onBack() {
        analytics.trackEvent(TrainingEvent.CompletionConfirmation.DismissRequested)
        navigateTo(ConfirmTrainingCompletionDirection.Back)
    }
}
