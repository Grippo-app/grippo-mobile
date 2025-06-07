package com.grippo.authorization.registration

import com.grippo.core.BaseViewModel
import com.grippo.presentation.api.profile.models.ExperienceEnumState
import kotlinx.collections.immutable.toPersistentList

internal class RegistrationViewModel :
    BaseViewModel<RegistrationState, RegistrationDirection, RegistrationLoader>(RegistrationState()),
    RegistrationContract {

    override fun saveCredentials(email: String, password: String) {
        update { it.copy(email = email, password = password) }
    }

    override fun saveName(name: String) {
        update { it.copy(name = name) }
    }

    override fun saveWeightHeight(weight: Float, height: Int) {
        update { it.copy(weight = weight, height = height) }
    }

    override fun saveExperience(experience: ExperienceEnumState) {
        update { it.copy(experience = experience) }
    }

    override fun saveExcludedMuscleIds(ids: List<String>) {
        val list = ids.toPersistentList()
        update { it.copy(excludedMuscleIds = list) }
    }

    override fun saveMissingEquipmentIds(ids: List<String>) {
        val list = ids.toPersistentList()
        update { it.copy(missingEquipmentIds = list) }
    }

    override fun back() {
        navigateTo(RegistrationDirection.Back)
    }
}