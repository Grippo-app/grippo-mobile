package com.grippo.authorization.registration

import com.grippo.core.state.profile.ExperienceEnumState

internal interface RegistrationContract {
    fun toNameWithCredentials(email: String, password: String)
    fun toBodyWithName(name: String)
    fun toExperienceWithBody(weight: Float, height: Int)
    fun toExcludedMusclesWithExperience(experience: ExperienceEnumState)
    fun toMissingEquipmentWithMuscles(ids: List<String>)
    fun toCompletedWithEquipment(ids: List<String>)
    fun toHome()
    fun onBack()
    fun onClose()

    companion object Empty : RegistrationContract {
        override fun toNameWithCredentials(email: String, password: String) {}
        override fun toBodyWithName(name: String) {}
        override fun toExperienceWithBody(weight: Float, height: Int) {}
        override fun toExcludedMusclesWithExperience(experience: ExperienceEnumState) {}
        override fun toMissingEquipmentWithMuscles(ids: List<String>) {}
        override fun toCompletedWithEquipment(ids: List<String>) {}
        override fun toHome() {}
        override fun onBack() {}
        override fun onClose() {}
    }
}