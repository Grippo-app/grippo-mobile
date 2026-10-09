package com.grippo.profile.menu

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.domain.state.user.toState
import kotlinx.coroutines.flow.onEach

internal class ProfileMenuViewModel(
    userFeature: UserFeature,
) : BaseViewModel<ProfileMenuState, ProfileMenuDirection, ProfileMenuLoader>(ProfileMenuState()),
    ProfileMenuContract {

    init {
        safeLaunch(loader = ProfileMenuLoader.User) {
            userFeature.getUser().getOrThrow()
        }

        userFeature.observeUser()
            .onEach(::provideUser)
            .safeLaunch()
    }

    private fun provideUser(user: User?) {
        update { it.copy(user = user?.toState()) }
    }

    override fun onProfileMenuClick(menu: ProfileMenu) {
        navigateTo(ProfileMenuDirection.ProfileMenuSelected(menu))
    }

    override fun onSettingsMenuClick(menu: SettingsMenu) {
        navigateTo(ProfileMenuDirection.SettingsMenuSelected(menu))
    }
}
