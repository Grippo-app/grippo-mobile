package com.grippo.profile.settings

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.user.UserFeature
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.account_deletion
import com.grippo.design.resources.provider.account_deletion_description
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController

internal class ProfileSettingsViewModel(
    private val userFeature: UserFeature,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider
) : BaseViewModel<ProfileSettingsState, ProfileSettingsDirection, ProfileSettingsLoader>(
    ProfileSettingsState
), ProfileSettingsContract {

    override fun onBack() {
        navigateTo(ProfileSettingsDirection.Back)
    }

    override fun onDeleteAccount() {
        safeLaunch {
            val config = DialogConfig.Confirmation(
                title = stringProvider.get(Res.string.account_deletion),
                description = stringProvider.get(Res.string.account_deletion_description),
                onResult = {
                    safeLaunch(loader = ProfileSettingsLoader.DeleteAccountButton) {
                        userFeature.deleteProfile().getOrThrow()
                    }
                }
            )
            dialogController.show(config)
        }
    }
}
