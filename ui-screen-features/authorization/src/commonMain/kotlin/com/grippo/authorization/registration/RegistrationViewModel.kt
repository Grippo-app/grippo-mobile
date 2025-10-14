package com.grippo.authorization.registration

import com.grippo.core.foundation.BaseViewModel
import com.grippo.state.profile.ExperienceEnumState
import kotlinx.collections.immutable.toPersistentList

internal class RegistrationViewModel :
    BaseViewModel<RegistrationState, RegistrationDirection, RegistrationLoader>(RegistrationState()),
    RegistrationContract {

    override fun onClose() {
        navigateTo(RegistrationDirection.Close)
    }

    override fun toNameWithCredentials(email: String, password: String) {
        update { it.copy(email = email, password = password) }
        navigateTo(RegistrationDirection.ToNameWithCredentials(email, password))
    }

    override fun toBodyWithName(name: String) {
        update { it.copy(name = name) }
        navigateTo(RegistrationDirection.ToBodyWithName(name))
    }

    override fun toExperienceWithBody(weight: Float, height: Int) {
        update { it.copy(weight = weight, height = height) }
        navigateTo(RegistrationDirection.ToExperienceWithBody(weight, height))
    }

    override fun toExcludedMusclesWithExperience(experience: ExperienceEnumState) {
        update { it.copy(experience = experience) }
        navigateTo(RegistrationDirection.ToExcludedMusclesWithExperience(experience))
    }

    override fun toMissingEquipmentWithMuscles(ids: List<String>) {
        val list = ids.toPersistentList()
        update { it.copy(excludedMuscleIds = list) }
        navigateTo(RegistrationDirection.ToMissingEquipmentWithMuscles(ids))
    }

    override fun toCompletedWithEquipment(ids: List<String>) {
        val list = ids.toPersistentList()
        update { it.copy(missingEquipmentIds = list) }
        navigateTo(RegistrationDirection.ToCompletedWithEquipment(ids))
    }

    override fun toHome() {
        navigateTo(RegistrationDirection.ToHome)
    }

    override fun onBack() {
        navigateTo(RegistrationDirection.Back)
    }
}