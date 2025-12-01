package com.grippo.authorization.profile.creation

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.profile.ExperienceEnumState

internal sealed interface ProfileCreationDirection : BaseDirection {
    data object Close : ProfileCreationDirection
    data class ToBodyWithName(val name: String) : ProfileCreationDirection
    data class ToExperienceWithBody(val weight: Float, val height: Int) : ProfileCreationDirection
    data class ToExcludedMusclesWithExperience(val experience: ExperienceEnumState) :
        ProfileCreationDirection

    data class ToMissingEquipmentWithMuscles(val ids: List<String>) : ProfileCreationDirection
    data class ToCompletedWithEquipment(val ids: List<String>) : ProfileCreationDirection
    data object ToHome : ProfileCreationDirection
    data object Back : ProfileCreationDirection
}
