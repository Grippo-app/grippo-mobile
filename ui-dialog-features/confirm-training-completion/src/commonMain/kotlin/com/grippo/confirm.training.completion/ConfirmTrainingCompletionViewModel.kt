package com.grippo.confirm.training.completion

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import kotlin.time.Duration

public class ConfirmTrainingCompletionViewModel(
    duration: Duration,
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
            initial = DurationFormatState.of(state.value.duration),
            onResult = { result ->
                val resolved = result.value ?: return@DurationPicker
                update { it.copy(duration = resolved) }
            }
        )
        dialogController.show(dialog)
    }

    override fun onBack() {
        navigateTo(ConfirmTrainingCompletionDirection.Back)
    }
}
