package com.grippo.authorization.registration.body

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController

internal class BodyViewModel(
    private val dialogController: DialogController
) : BaseViewModel<BodyState, BodyDirection, BodyLoader>(BodyState()),
    BodyContract {

    override fun openWeightPicker() {
        val dialog = DialogConfig.WeightPicker(
            initial = state.value.weight,
            onResult = { value -> update { it.copy(weight = value) } }
        )
        dialogController.show(dialog)
    }

    override fun openHeightPicker() {
        val dialog = DialogConfig.HeightPicker(
            initial = state.value.height,
            onResult = { value -> update { it.copy(height = value) } }
        )
        dialogController.show(dialog)
    }

    override fun next() {
        val direction = BodyDirection.Experience(
            weight = state.value.weight,
            height = state.value.height
        )
        navigateTo(direction)
    }

    override fun back() {
        navigateTo(BodyDirection.Back)
    }
}