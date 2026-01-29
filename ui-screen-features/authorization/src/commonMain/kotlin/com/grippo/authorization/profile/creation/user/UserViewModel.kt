package com.grippo.authorization.profile.creation.user

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.NameFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController

internal class UserViewModel(
    private val dialogController: DialogController,
    private val authorizationFeature: AuthorizationFeature
) : BaseViewModel<UserState, UserDirection, UserLoader>(UserState()),
    UserContract {

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

    override fun onNameChange(value: String) {
        update { it.copy(name = NameFormatState.of(value)) }
    }

    override fun onNextClick() {
        val name = (state.value.name as? NameFormatState.Valid) ?: return
        val weight = (state.value.weight as? WeightFormatState.Valid) ?: return
        val height = (state.value.height as? HeightFormatState.Valid) ?: return

        val direction = UserDirection.Experience(
            name = name.value,
            weight = weight.value,
            height = height.value
        )
        navigateTo(direction)
    }

    override fun onBack() {
        safeLaunch {
            authorizationFeature.logout()
            navigateTo(UserDirection.Back)
        }
    }
}