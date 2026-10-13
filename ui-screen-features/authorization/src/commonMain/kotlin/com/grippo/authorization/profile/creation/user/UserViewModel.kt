package com.grippo.authorization.profile.creation.user

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.analytics.authorization.models.ProfilePicker
import com.grippo.analytics.common.models.ControlAction
import com.grippo.analytics.common.models.InputKeyboardAction
import com.grippo.authorization.analytics.AuthInputMapping
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
import com.grippo.toolkit.analytics.context.NavigationReason

internal class UserViewModel(
    private val analytics: AuthAnalytics,
    private val dialogController: DialogController,
    private val logoutUseCase: LogoutUseCase,
    initialState: UserState = UserState(),
) : BaseViewModel<UserState, UserDirection, UserLoader>(initialState),
    UserContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(AuthScreen.User, reason)
    }

    override fun onWeightPickerClick() {
        if (loaders.value.isNotEmpty()) {
            return
        }

        analytics.trackEvent(AuthEvent.User.PickerOpened(ProfilePicker.Weight))
        val dialog = DialogConfig.WeightPicker(
            title = UiText.Res(Res.string.weight_picker_title),
            initial = state.value.weight.value,
            onResult = { value ->
                val input = WeightFormatState.of(value)
                update { it.copy(weight = input) }
                analytics.trackEvent(
                    AuthEvent.User.PickerConfirmed(
                        ProfilePicker.Weight,
                        AuthInputMapping.status(input),
                    )
                )
            }
        )
        dialogController.open(dialog)
    }

    override fun onHeightPickerClick() {
        if (loaders.value.isNotEmpty()) {
            return
        }

        analytics.trackEvent(AuthEvent.User.PickerOpened(ProfilePicker.Height))
        val dialog = DialogConfig.HeightPicker(
            title = UiText.Res(Res.string.height_picker_title),
            initial = state.value.height.value,
            onResult = { value ->
                val input = HeightFormatState.of(value)
                update { it.copy(height = input) }
                analytics.trackEvent(
                    AuthEvent.User.PickerConfirmed(
                        ProfilePicker.Height,
                        AuthInputMapping.status(input),
                    )
                )
            }
        )
        dialogController.open(dialog)
    }

    override fun onNameChange(value: String) {
        val previousStatus = AuthInputMapping.status(state.value.name)
        val input = NameFormatState.of(value)
        update { it.copy(name = input) }
        val status = AuthInputMapping.status(input)
        if (status != previousStatus) {
            analytics.trackEvent(AuthEvent.User.NameInputChanged(status))
        }
    }

    override fun onNameFocused() {
        analytics.trackEvent(AuthEvent.User.NameInputFocused)
    }

    override fun onNameCleared() {
        analytics.trackEvent(AuthEvent.User.NameInputCleared)
    }

    override fun onNameKeyboardDone() {
        analytics.trackEvent(
            AuthEvent.User.NameKeyboardAction(InputKeyboardAction.Done)
        )
    }

    override fun onNextClick() {
        if (loaders.value.isNotEmpty()) {
            return
        }

        val current = state.value
        val name = (current.name as? NameFormatState.Valid) ?: return
        val weight = (current.weight as? WeightFormatState.Valid) ?: return
        val height = (current.height as? HeightFormatState.Valid) ?: return

        analytics.trackEvent(AuthEvent.User.Control(ControlAction.Next))

        val direction = UserDirection.Experience(
            name = name.value,
            weight = weight.value,
            height = height.value
        )
        navigateTo(direction)
    }

    override fun onBack() {
        if (loaders.value.isNotEmpty()) {
            return
        }

        analytics.trackEvent(AuthEvent.User.Control(ControlAction.Back))
        safeLaunch(loader = UserLoader.Logout) {
            analytics.trackOperation(AuthOperation.Logout) {
                logoutUseCase.execute()
            }
            navigateTo(UserDirection.Back)
        }
    }
}
