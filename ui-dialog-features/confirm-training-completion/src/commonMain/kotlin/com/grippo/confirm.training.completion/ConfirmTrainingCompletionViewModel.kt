package com.grippo.confirm.training.completion

import com.grippo.core.foundation.BaseViewModel
import kotlin.time.Duration

public class ConfirmTrainingCompletionViewModel(
    duration: Duration,
) : BaseViewModel<ConfirmTrainingCompletionState, ConfirmTrainingCompletionDirection, ConfirmTrainingCompletionLoader>(
    ConfirmTrainingCompletionState(
        duration = duration
    )
), ConfirmTrainingCompletionContract {

    override fun onConfirm() {
        navigateTo(ConfirmTrainingCompletionDirection.Confirm)
    }

    override fun onDurationInputClick() {

    }

    override fun onBack() {
        navigateTo(ConfirmTrainingCompletionDirection.Back)
    }
}
