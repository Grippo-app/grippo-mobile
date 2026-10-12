package com.grippo.authorization.profile.creation

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.ExperienceEnumState
import kotlinx.collections.immutable.toPersistentList

internal class ProfileCreationViewModel :
    BaseViewModel<ProfileCreationState, ProfileCreationDirection, ProfileCreationLoader>(
        ProfileCreationState()
    ),
    ProfileCreationContract {

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
