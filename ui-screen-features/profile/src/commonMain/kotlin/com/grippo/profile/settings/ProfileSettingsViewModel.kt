package com.grippo.profile.settings

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.UiText
import com.grippo.data.features.api.authorization.DeleteProfileUseCase
import com.grippo.data.features.api.authorization.LogoutUseCase
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.account_deletion
import com.grippo.design.resources.provider.account_deletion_description
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController

internal class ProfileSettingsViewModel(
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val dialogController: DialogController,
) : BaseViewModel<ProfileSettingsState, ProfileSettingsDirection, ProfileSettingsLoader>(
    ProfileSettingsState
), ProfileSettingsContract {

    override fun onBack() {
        navigateTo(ProfileSettingsDirection.Back)
    }

    override fun onLogoutClick() {
        safeLaunch(loader = ProfileSettingsLoader.LogoutButton) {
            logoutUseCase.execute()
        }
    }

    override fun onDeleteAccount() {
        val config = DialogConfig.Confirmation(
            title = UiText.Res(Res.string.account_deletion),
            description = UiText.Res(Res.string.account_deletion_description),
            onResult = {
                safeLaunch(loader = ProfileSettingsLoader.DeleteAccountButton) {
                    deleteProfileUseCase.execute()
                }
            })
        dialogController.open(config)
    }
}
