package com.grippo.profile.body

import com.grippo.core.foundation.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController

internal class ProfileBodyViewModel(
    private val dialogController: DialogController
) : BaseViewModel<ProfileBodyState, ProfileBodyDirection, ProfileBodyLoader>(
    ProfileBodyState()
), ProfileBodyContract {

    override fun onWeightPickerClick() {
        val dialog = DialogConfig.WeightPicker(
            initial = state.value.weight,
            onResult = { value -> update { it.copy(weight = value) } }
        )
        dialogController.show(dialog)
    }

    override fun onBack() {
        navigateTo(ProfileBodyDirection.Back)
    }
}