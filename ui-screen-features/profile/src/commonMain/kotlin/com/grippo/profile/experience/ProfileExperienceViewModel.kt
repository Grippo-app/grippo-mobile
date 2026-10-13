package com.grippo.profile.experience

import com.grippo.analytics.profile.ProfileAnalytics
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.user.UserFeature
import com.grippo.domain.state.user.toState
import com.grippo.state.domain.user.toDomain
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take

internal class ProfileExperienceViewModel(
    private val analytics: ProfileAnalytics,
    initialState: ProfileExperienceState?,
    private val userFeature: UserFeature
) : BaseViewModel<ProfileExperienceState, ProfileExperienceDirection, ProfileExperienceLoader>(
    initialState ?: ProfileExperienceState()
), ProfileExperienceContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(ProfileScreen.Experience, reason)
    }

    init {
        if (initialState == null) {
            userFeature.observeUser()
                .mapNotNull { it?.experience }
                .take(1)
                .onEach(::provideExperience)
                .safeLaunch(processing = Processing.Infinity)
        }

        safeLaunch {
            userFeature.getUser().getOrThrow()
        }
    }

    private fun provideExperience(value: ExperienceEnum) {
        val experience = value.toState()
        update { if (it.selected == null) it.copy(selected = experience) else it }
    }

    override fun onSelectExperience(value: ExperienceEnumState) {
        update { it.copy(selected = value) }
    }

    override fun onApply() {
        val selected = state.value.selected ?: return

        safeLaunch(loader = ProfileExperienceLoader.ApplyButton) {
            userFeature.setExperience(selected.toDomain()).getOrThrow()
            navigateTo(ProfileExperienceDirection.Back)
        }
    }

    override fun onBack() {
        navigateTo(ProfileExperienceDirection.Back)
    }
}
