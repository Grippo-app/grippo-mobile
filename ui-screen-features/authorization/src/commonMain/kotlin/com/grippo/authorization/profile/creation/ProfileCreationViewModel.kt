package com.grippo.authorization.profile.creation

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.data.features.api.authorization.AuthorizationFeature
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

internal class ProfileCreationViewModel(
    private val authorizationFeature: AuthorizationFeature,
    initialState: ProfileCreationState = ProfileCreationState(),
) : BaseViewModel<ProfileCreationState, ProfileCreationDirection, ProfileCreationLoader>(
    initialState
), ProfileCreationContract {

    init {
        observeToken()
    }

    private fun observeToken() {
        authorizationFeature.getToken()
            .onEach(::provideToken)
            .safeLaunch(onError = ::observeToken)
    }

    private fun provideToken(value: String?) {
        if (value == null) {
            toLogin()
        }
    }

    override fun onClose() {
        navigateTo(ProfileCreationDirection.Close)
    }

    override fun toExperienceWithUser(name: String, weight: Float, height: Int) {
        update { it.copy(weight = weight, height = height, name = name) }
        navigateTo(ProfileCreationDirection.ToExperience)
    }

    override fun toExcludedMusclesWithExperience(experience: ExperienceEnumState) {
        update { it.copy(experience = experience) }
        navigateTo(ProfileCreationDirection.ToExcludedMuscles)
    }

    override fun toMissingEquipmentWithMuscles(ids: List<String>) {
        update { it.copy(excludedMuscleIds = ids.toPersistentList()) }
        navigateTo(ProfileCreationDirection.ToMissingEquipments)
    }

    override fun toCompletedWithEquipment(ids: List<String>) {
        update { it.copy(missingEquipmentIds = ids.toPersistentList()) }
        navigateTo(ProfileCreationDirection.ToCompleted)
    }

    override fun toHome() {
        navigateTo(ProfileCreationDirection.ToHome)
    }

    override fun onBack() {
        navigateTo(ProfileCreationDirection.Back)
    }

    override fun toLogin() {
        navigateTo(ProfileCreationDirection.ToLogin)
    }
}
