package com.grippo.authorization.registration.excluded.muscles

import com.grippo.core.models.BaseDirection

internal sealed interface ExcludedMusclesDirection : BaseDirection {
    data class MissingEquipment(val muscleIds: List<String>) : ExcludedMusclesDirection
}