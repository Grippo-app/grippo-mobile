package com.grippo.dialog.profile

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.ProfileActivityMenu
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.domain.state.user.toState
import kotlinx.coroutines.flow.onEach

public class ProfileViewModel(
    userFeature: UserFeature,
    private val authorizationFeature: AuthorizationFeature
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

    override fun onLogoutClick() {
        safeLaunch {
            authorizationFeature.logout()
            navigateTo(ProfileDirection.Back)
        }
    }

    override fun onActivityMenuClick(menu: ProfileActivityMenu) {
        navigateTo(ProfileDirection.BackWithResult(menu))
    }

    override fun onBack() {
        navigateTo(ProfileDirection.Back)
    }
}