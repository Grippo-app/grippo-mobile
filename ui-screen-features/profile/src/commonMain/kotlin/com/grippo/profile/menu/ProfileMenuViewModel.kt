package com.grippo.profile.menu

import com.grippo.analytics.profile.ProfileAnalytics
import com.grippo.analytics.profile.models.ProfileEvent
import com.grippo.analytics.profile.models.ProfileOperation
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu
import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.domain.state.user.toState
import com.grippo.profile.analytics.ProfileInputMapping
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlinx.coroutines.flow.onEach

internal class ProfileMenuViewModel(
    private val analytics: ProfileAnalytics,
    userFeature: UserFeature,
    goalFeature: GoalFeature,
    excludedMusclesFeature: ExcludedMusclesFeature,
    excludedEquipmentsFeature: ExcludedEquipmentsFeature,
) : BaseViewModel<ProfileMenuState, ProfileMenuDirection, ProfileMenuLoader>(ProfileMenuState()),
    ProfileMenuContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(ProfileScreen.Menu, reason)
    }

    init {
        safeLaunch(loader = ProfileMenuLoader.User) {
            analytics.trackOperation(ProfileOperation.RefreshUser) {
                userFeature.getUser().getOrThrow()
            }
        }

        userFeature.observeUser()
            .onEach(::provideUser)
            .safeLaunch()

        goalFeature.observeGoal()
            .onEach(::provideGoal)
            .safeLaunch()

        excludedMusclesFeature.observeExcludedMuscles()
            .onEach(::provideExcludedMuscles)
            .safeLaunch()

        excludedEquipmentsFeature.observeExcludedEquipments()
            .onEach(::provideExcludedEquipments)
            .safeLaunch()

        safeLaunch(loader = ProfileMenuLoader.Goal) {
            analytics.trackOperation(ProfileOperation.RefreshGoal) {
                goalFeature.getGoal().getOrThrow()
            }
        }

        safeLaunch(loader = ProfileMenuLoader.Muscles) {
            analytics.trackOperation(ProfileOperation.RefreshMuscles) {
                excludedMusclesFeature.getExcludedMuscles().getOrThrow()
            }
        }

        safeLaunch(loader = ProfileMenuLoader.Equipments) {
            analytics.trackOperation(ProfileOperation.RefreshEquipments) {
                excludedEquipmentsFeature.getExcludedEquipments().getOrThrow()
            }
        }
    }

    private fun provideUser(value: User?) {
        val user = value?.toState()

        update { it.copy(user = user) }
    }

    private fun provideGoal(value: Goal?) {
        val goal = value?.toState()

        update { it.copy(goal = goal) }
    }

    private fun provideExcludedMuscles(value: List<Muscle>) {
        val count = value.size

        update { it.copy(excludedMusclesCount = count) }
    }

    private fun provideExcludedEquipments(value: List<Equipment>) {
        val count = value.size

        update { it.copy(excludedEquipmentsCount = count) }
    }

    override fun onProfileMenuClick(menu: ProfileMenu) {
        val target = ProfileInputMapping.target(menu)

        val event = ProfileEvent.MenuSelected(target = target)
        analytics.trackEvent(event)

        val direction = ProfileMenuDirection.ProfileMenuSelected(item = menu)
        navigateTo(direction)
    }

    override fun onSettingsMenuClick(menu: SettingsMenu) {
        val target = ProfileInputMapping.target(menu)

        val event = ProfileEvent.MenuSelected(target = target)
        analytics.trackEvent(event)

        val direction = ProfileMenuDirection.SettingsMenuSelected(item = menu)
        navigateTo(direction)
    }
}
