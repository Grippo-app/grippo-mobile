package com.grippo.dialog.profile

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.domain.state.user.toState
import kotlinx.coroutines.flow.onEach

public class ProfileViewModel(
    userFeature: UserFeature,
) : BaseViewModel<ProfileState, ProfileDirection, ProfileLoader>(ProfileState()),
    ProfileContract {

    init {
        userFeature.observeUser()
            .onEach(::provideUser)
            .safeLaunch()
    }

    private fun provideUser(user: User?) {
        update { it.copy(user = user?.toState()) }
    }

    override fun onProfileMenuClick(menu: ProfileMenu) {
        navigateTo(ProfileDirection.BackWithProfileMenuResult(menu))
    }

    override fun onSettingsMenuClick(menu: SettingsMenu) {
        navigateTo(ProfileDirection.BackWithSettingsMenuResult(menu))
    }

    override fun onBack() {
        navigateTo(ProfileDirection.Back)
    }
}