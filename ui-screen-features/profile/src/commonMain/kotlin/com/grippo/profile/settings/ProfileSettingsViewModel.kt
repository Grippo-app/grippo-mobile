package com.grippo.profile.settings

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.user.UserFeature

internal class ProfileSettingsViewModel(
    private val userFeature: UserFeature
) : BaseViewModel<ProfileSettingsState, ProfileSettingsDirection, ProfileSettingsLoader>(
    ProfileSettingsState
), ProfileSettingsContract {

    override fun onBack() {
        navigateTo(ProfileSettingsDirection.Back)
    }

    override fun onDeleteAccount() {
        safeLaunch(loader = ProfileSettingsLoader.DeleteAccountButton) {
            userFeature.deleteProfile().getOrThrow()
        }
    }
}
