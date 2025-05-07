package com.grippo.authorization.registration.experience

import com.grippo.core.models.BaseDirection
import com.grippo.presentation.api.user.models.Experience

internal sealed interface ExperienceDirection : BaseDirection {
    data class ExcludedMuscles(val experience: Experience) : ExperienceDirection
}