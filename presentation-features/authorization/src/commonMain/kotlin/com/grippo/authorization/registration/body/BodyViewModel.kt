package com.grippo.authorization.registration.body

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import org.koin.core.component.inject

internal class BodyViewModel : BaseViewModel<BodyState, BodyDirection, BodyLoader>(BodyState()),
    BodyContract {

    private val dialogController by inject<DialogController>()
    override fun openWeightPicker() {
        dialogController.show(DialogConfig.WeightPicker(state.value.weight))
    }

    override fun openHeightPicker() {
        dialogController.show(DialogConfig.HeightPicker(state.value.height))
    }

    override fun next() {

    }
}