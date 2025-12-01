package com.grippo.authorization.profile.creation.experience

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.profile.ExperienceEnumState

internal sealed interface ExperienceDirection : BaseDirection {
    data class ExcludedMuscles(val experience: ExperienceEnumState) : ExperienceDirection
    data object Back : ExperienceDirection
}