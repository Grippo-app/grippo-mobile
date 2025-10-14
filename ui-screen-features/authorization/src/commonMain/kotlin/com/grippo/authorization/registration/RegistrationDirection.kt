package com.grippo.authorization.registration

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.profile.ExperienceEnumState

internal sealed interface RegistrationDirection : BaseDirection {
    data object Close : RegistrationDirection
    data class ToNameWithCredentials(val email: String, val password: String) :
        RegistrationDirection

    data class ToBodyWithName(val name: String) : RegistrationDirection
    data class ToExperienceWithBody(val weight: Float, val height: Int) : RegistrationDirection
    data class ToExcludedMusclesWithExperience(val experience: ExperienceEnumState) :
        RegistrationDirection

    data class ToMissingEquipmentWithMuscles(val ids: List<String>) : RegistrationDirection
    data class ToCompletedWithEquipment(val ids: List<String>) : RegistrationDirection
    data object ToHome : RegistrationDirection
    data object Back : RegistrationDirection
}