package com.grippo.authorization.registration.body

import com.grippo.core.BaseViewModel
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
        val direction = BodyDirection.Experience(
            weight = state.value.weight.value ?: 0f,
            height = state.value.height.value ?: 0
        )
        navigateTo(direction)
    }

    override fun onBack() {
        navigateTo(BodyDirection.Back)
    }
}