package com.grippo.confirm.training.completion

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController

public class ConfirmTrainingCompletionViewModel(
    initial: DurationFormatState,
    private val dialogController: DialogController
) : BaseViewModel<ConfirmTrainingCompletionState, ConfirmTrainingCompletionDirection, ConfirmTrainingCompletionLoader>(
    ConfirmTrainingCompletionState(
        duration = initial
    )
), ConfirmTrainingCompletionContract {

    override fun onConfirm() {
        navigateTo(ConfirmTrainingCompletionDirection.Confirm(state.value.duration))
    }

    override fun onDurationInputClick() {
        val dialog = DialogConfig.DurationPicker(
            initial = state.value.duration.value,
            onResult = { result -> update { it.copy(duration = DurationFormatState.of(result)) } }
        )

        dialogController.show(dialog)
    }

    override fun onBack() {
        navigateTo(ConfirmTrainingCompletionDirection.Back)
    }
}
