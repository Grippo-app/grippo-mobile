package com.grippo.home.profile

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.domain.state.user.toState
import kotlinx.coroutines.flow.onEach

internal class HomeProfileViewModel(
    userFeature: UserFeature, private val authorizationFeature: AuthorizationFeature
) : BaseViewModel<HomeProfileState, HomeProfileDirection, HomeProfileLoader>(HomeProfileState()),
    HomeProfileContract {

    init {
        userFeature.observeUser()
            .onEach(::provideUser)
            .safeLaunch()
    }

    private fun provideUser(user: User?) {
        update { it.copy(user = user?.toState()) }
    }

    override fun onLogoutClick() {
        safeLaunch { authorizationFeature.logout() }
    }

    override fun onActivityMenuClick(menu: HomeProfileActivityMenu) {
        val direction = when (menu) {
            HomeProfileActivityMenu.ExcludedMuscles -> HomeProfileDirection.ExcludedMuscles
            HomeProfileActivityMenu.MissingEquipment -> HomeProfileDirection.MissingEquipment
        }

        navigateTo(direction)
    }

    override fun onSettingsMenuClick(menu: HomeProfileSettingsMenu) {
        val direction = when (menu) {
            HomeProfileSettingsMenu.System -> HomeProfileDirection.SystemSettings
            HomeProfileSettingsMenu.Debug -> HomeProfileDirection.Debug
        }

        navigateTo(direction)
    }

    override fun onStartWorkoutClick() {
        navigateTo(HomeProfileDirection.Workout)
    }

    override fun onBack() {
        navigateTo(HomeProfileDirection.Back)
    }
}