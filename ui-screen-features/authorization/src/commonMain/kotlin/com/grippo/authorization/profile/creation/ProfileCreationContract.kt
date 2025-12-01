package com.grippo.authorization.profile.creation

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.ExperienceEnumState

@Immutable
internal interface ProfileCreationContract {
    fun toBodyWithName(name: String)
    fun toExperienceWithBody(weight: Float, height: Int)
    fun toExcludedMusclesWithExperience(experience: ExperienceEnumState)
    fun toMissingEquipmentWithMuscles(ids: List<String>)
    fun toCompletedWithEquipment(ids: List<String>)
    fun toHome()
    fun onBack()
    fun onClose()

    @Immutable
    companion object Empty : ProfileCreationContract {
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
