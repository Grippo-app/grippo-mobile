package com.grippo.authorization.registration.body

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController

internal class BodyViewModel(
    private val dialogController: DialogController
) : BaseViewModel<BodyState, BodyDirection, BodyLoader>(BodyState()),
    BodyContract {

    override fun onWeightPickerClick() {
        val dialog = DialogConfig.WeightPicker(
            initial = state.value.weight,
            onResult = { value -> update { it.copy(weight = value) } }
        )
        dialogController.show(dialog)
    }

    override fun onHeightPickerClick() {
        val dialog = DialogConfig.HeightPicker(
            initial = state.value.height,
            onResult = { value -> update { it.copy(height = value) } }
        )
        dialogController.show(dialog)
    }

    override fun onNextClick() {
        val weight = (state.value.weight as? WeightFormatState.Valid) ?: return
        val height = (state.value.height as? HeightFormatState.Valid) ?: return

        val direction = BodyDirection.Experience(
            weight = weight.value,
            height = height.value
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(BodyDirection.Back)
    }
}