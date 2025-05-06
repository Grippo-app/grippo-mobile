package com.grippo.authorization.registration.body

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import org.koin.core.component.inject

internal class BodyViewModel : BaseViewModel<BodyState, BodyDirection, BodyLoader>(BodyState()),
    BodyContract {

    private val dialogController by inject<DialogController>()

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

    }
}