package com.grippo.authorization.registration.experience

import com.grippo.core.models.BaseDirection
import com.grippo.presentation.api.profile.models.ExperienceEnumState

internal sealed interface ExperienceDirection : BaseDirection {
    data class ExcludedMuscles(val experience: ExperienceEnumState) : ExperienceDirection
    data object Back : ExperienceDirection
}