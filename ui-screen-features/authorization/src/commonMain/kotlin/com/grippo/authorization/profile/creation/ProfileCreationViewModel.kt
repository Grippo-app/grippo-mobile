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
        navigateTo(ProfileCreationDirection.ToExperienceWithBody(name, weight, height))
    }

    override fun toExcludedMusclesWithExperience(experience: ExperienceEnumState) {
        update { it.copy(experience = experience) }
        navigateTo(ProfileCreationDirection.ToExcludedMusclesWithExperience(experience))
    }

    override fun toMissingEquipmentWithMuscles(ids: List<String>) {
        val list = ids.toPersistentList()
        update { it.copy(excludedMuscleIds = list) }
        navigateTo(ProfileCreationDirection.ToMissingEquipmentWithMuscles(ids))
    }

    override fun toCompletedWithEquipment(ids: List<String>) {
        val list = ids.toPersistentList()
        update { it.copy(missingEquipmentIds = list) }
        navigateTo(ProfileCreationDirection.ToCompletedWithEquipment(ids))
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
