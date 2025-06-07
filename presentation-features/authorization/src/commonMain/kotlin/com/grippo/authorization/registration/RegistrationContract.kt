package com.grippo.authorization.registration

import com.grippo.presentation.api.profile.models.ExperienceEnumState

internal interface RegistrationContract {
    fun saveCredentials(email: String, password: String)
    fun saveName(name: String)
    fun saveWeightHeight(weight: Float, height: Int)
    fun saveExperience(experience: ExperienceEnumState)
    fun saveExcludedMuscleIds(ids: List<String>)
    fun saveMissingEquipmentIds(ids: List<String>)
    fun back()

    companion object Empty : RegistrationContract {
        override fun saveCredentials(email: String, password: String) {}
        override fun saveName(name: String) {}
        override fun saveWeightHeight(weight: Float, height: Int) {}
        override fun saveExperience(experience: ExperienceEnumState) {}
        override fun saveExcludedMuscleIds(ids: List<String>) {}
        override fun saveMissingEquipmentIds(ids: List<String>) {}
        override fun back() {}
    }
}