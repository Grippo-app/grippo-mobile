package com.grippo.authorization.registration.excluded.muscles

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface ExcludedMusclesDirection : BaseDirection {
    data class MissingEquipment(val excludedMuscleIds: List<String>) : ExcludedMusclesDirection
    data object Back : ExcludedMusclesDirection
}