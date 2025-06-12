package com.grippo.authorization.registration.body

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.presentation.api.profile.models.HeightFormatState
import com.grippo.presentation.api.profile.models.WeightFormatState

internal class BodyViewModel(
    private val dialogController: DialogController
) : BaseViewModel<BodyState, BodyDirection, BodyLoader>(BodyState()),
    BodyContract {

    override fun openWeightPicker() {
        val dialog = DialogConfig.WeightPicker(
            initial = state.value.weight.value,
            onResult = { value -> update { it.copy(weight = WeightFormatState.of(value)) } }
        )
        dialogController.show(dialog)
    }

    override fun openHeightPicker() {
        val dialog = DialogConfig.HeightPicker(
            initial = state.value.height.value,
            onResult = { value -> update { it.copy(height = HeightFormatState.of(value)) } }
        )
        dialogController.show(dialog)
    }

    override fun next() {
        val direction = BodyDirection.Experience(
            weight = state.value.weight.value,
            height = state.value.height.value
        )
        navigateTo(direction)
    }

    override fun back() {
        navigateTo(BodyDirection.Back)
    }
}