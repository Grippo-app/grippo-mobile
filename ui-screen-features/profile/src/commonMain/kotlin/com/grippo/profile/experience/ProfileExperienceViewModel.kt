package com.grippo.profile.experience

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.domain.state.user.toState
import com.grippo.state.domain.user.toDomain
import kotlinx.coroutines.flow.onEach

internal class ProfileExperienceViewModel(
    private val userFeature: UserFeature
) : BaseViewModel<ProfileExperienceState, ProfileExperienceDirection, ProfileExperienceLoader>(
    ProfileExperienceState()
), ProfileExperienceContract {

    init {
        userFeature.observeUser()
            .onEach(::provideUser)
            .safeLaunch()

        safeLaunch {
            userFeature.getUser().getOrThrow()
        }
    }

    private fun provideUser(user: User?) {
        val experience = user?.experience?.toState() ?: return
        update { it.copy(selected = experience) }
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
