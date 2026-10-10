package com.grippo.profile.menu

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu
import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.domain.state.user.toState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class ProfileMenuViewModel(
    userFeature: UserFeature,
    goalFeature: GoalFeature,
    excludedMusclesFeature: ExcludedMusclesFeature,
    excludedEquipmentsFeature: ExcludedEquipmentsFeature,
) : BaseViewModel<ProfileMenuState, ProfileMenuDirection, ProfileMenuLoader>(ProfileMenuState()),
    ProfileMenuContract {

    init {
        safeLaunch(loader = ProfileMenuLoader.User) {
            userFeature.getUser().getOrThrow()
        }

        userFeature.observeUser()
            .onEach(::provideUser)
            .safeLaunch()

        goalFeature.observeGoal()
            .map { it?.toState() }
            .distinctUntilChanged()
            .onEach { goal -> update { it.copy(goal = goal) } }
            .safeLaunch()

        excludedMusclesFeature.observeExcludedMuscles()
            .map { it.size }
            .distinctUntilChanged()
            .onEach { count -> update { it.copy(excludedMusclesCount = count) } }
            .safeLaunch()

        excludedEquipmentsFeature.observeExcludedEquipments()
            .map { it.size }
            .distinctUntilChanged()
            .onEach { count -> update { it.copy(excludedEquipmentsCount = count) } }
            .safeLaunch()

        safeLaunch(loader = ProfileMenuLoader.Goal) {
            goalFeature.getGoal().getOrThrow()
        }

        safeLaunch(loader = ProfileMenuLoader.Muscles) {
            excludedMusclesFeature.getExcludedMuscles().getOrThrow()
        }

        safeLaunch(loader = ProfileMenuLoader.Equipments) {
            excludedEquipmentsFeature.getExcludedEquipments().getOrThrow()
        }
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
