package com.grippo.confirm.training.completion

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController

public class ConfirmTrainingCompletionViewModel(
    duration: DurationFormatState,
    private val dialogController: DialogController
) : BaseViewModel<ConfirmTrainingCompletionState, ConfirmTrainingCompletionDirection, ConfirmTrainingCompletionLoader>(
    ConfirmTrainingCompletionState(
        duration = duration
    )
), ConfirmTrainingCompletionContract {

    override fun onConfirm() {
        navigateTo(ConfirmTrainingCompletionDirection.Confirm(state.value.duration))
    }

    override fun onDurationInputClick() {
        val dialog = DialogConfig.DurationPicker(
            initial = state.value.duration,
            onResult = { result -> update { it.copy(duration = result) } }
        )

        dialogController.show(dialog)
    }

    override fun onBack() {
        navigateTo(ConfirmTrainingCompletionDirection.Back)
    }
}
