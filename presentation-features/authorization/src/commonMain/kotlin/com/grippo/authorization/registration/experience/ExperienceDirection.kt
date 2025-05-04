package com.grippo.authorization.registration.experience

import com.grippo.core.models.BaseDirection

internal sealed interface ExperienceDirection : BaseDirection {
    data object ExcludedMuscles : ExperienceDirection
}