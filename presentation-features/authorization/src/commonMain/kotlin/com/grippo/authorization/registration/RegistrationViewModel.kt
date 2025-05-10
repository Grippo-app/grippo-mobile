package com.grippo.authorization.registration

import com.grippo.core.BaseViewModel
import com.grippo.presentation.api.user.models.Experience
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

    override fun saveExperience(experience: Experience) {
        update { it.copy(experience = experience) }
    }

    override fun saveExcludedMuscleIds(ids: List<String>) {
        val list = ids.toPersistentList()
        update { it.copy(excludedMuscleIds = list) }
    }

    override fun saveMissingEquipmentsIds(ids: List<String>) {
        val list = ids.toPersistentList()
        update { it.copy(mussingEquipmentIds = list) }
    }
}