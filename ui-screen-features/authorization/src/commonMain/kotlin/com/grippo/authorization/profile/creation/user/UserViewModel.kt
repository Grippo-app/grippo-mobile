package com.grippo.authorization.profile.creation.user

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.NameFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.data.features.api.authorization.LogoutUseCase
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.height_picker_title
import com.grippo.design.resources.provider.weight_picker_title
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController

internal class UserViewModel(
    private val dialogController: DialogController,
    private val logoutUseCase: LogoutUseCase
) : BaseViewModel<UserState, UserDirection, UserLoader>(UserState()),
    UserContract {

    override fun onWeightPickerClick() {
        val dialog = DialogConfig.WeightPicker(
            title = UiText.Res(Res.string.weight_picker_title),
            initial = state.value.weight.value,
            onResult = { value -> update { it.copy(weight = WeightFormatState.of(value)) } }
        )
        dialogController.open(dialog)
    }

    override fun onHeightPickerClick() {
        val dialog = DialogConfig.HeightPicker(
            title = UiText.Res(Res.string.height_picker_title),
            initial = state.value.height.value,
            onResult = { value -> update { it.copy(height = HeightFormatState.of(value)) } }
        )
        dialogController.open(dialog)
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
            logoutUseCase.execute()
            navigateTo(UserDirection.Back)
        }
    }
}
