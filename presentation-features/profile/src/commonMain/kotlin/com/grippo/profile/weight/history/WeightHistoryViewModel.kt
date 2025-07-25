package com.grippo.profile.weight.history

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.presentation.api.profile.models.WeightFormatState

internal class WeightHistoryViewModel(
    private val dialogController: DialogController
) : BaseViewModel<WeightHistoryState, WeightHistoryDirection, WeightHistoryLoader>(
    WeightHistoryState()
), WeightHistoryContract {

    override fun openWeightPicker() {
        val dialog = DialogConfig.WeightPicker(
            initial = state.value.weight.value,
            onResult = { value -> update { it.copy(weight = WeightFormatState.of(value)) } }
        )
        dialogController.show(dialog)
    }

    override fun back() {
        navigateTo(WeightHistoryDirection.Back)
    }
}