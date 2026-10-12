package com.grippo.authorization.profile.creation

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface ProfileCreationDirection : BaseDirection {
    data object Close : ProfileCreationDirection
    data object ToExperience : ProfileCreationDirection
    data object ToExcludedMuscles : ProfileCreationDirection
    data object ToMissingEquipments : ProfileCreationDirection
    data object ToCompleted : ProfileCreationDirection
    data object ToHome : ProfileCreationDirection
    data object Back : ProfileCreationDirection
    data object ToLogin : ProfileCreationDirection
}
