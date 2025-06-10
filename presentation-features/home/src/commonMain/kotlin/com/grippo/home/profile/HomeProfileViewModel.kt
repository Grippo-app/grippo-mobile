package com.grippo.home.profile

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.domain.mapper.user.toState
import kotlinx.coroutines.flow.onEach

internal class HomeProfileViewModel(
    userFeature: UserFeature,
    private val authorizationFeature: AuthorizationFeature
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

    override fun onMenuClick(menu: HomeProfileMenu) {
        val direction = when (menu) {
            HomeProfileMenu.WeightHistory -> HomeProfileDirection.WeightHistory
            HomeProfileMenu.ExcludedMuscles -> HomeProfileDirection.ExcludedMuscles
            HomeProfileMenu.MissingEquipment -> HomeProfileDirection.MissingEquipment
            HomeProfileMenu.ExerciseLibrary -> HomeProfileDirection.ExerciseLibrary
        }

        navigateTo(direction)
    }

    override fun back() {
        navigateTo(HomeProfileDirection.Back)
    }
}