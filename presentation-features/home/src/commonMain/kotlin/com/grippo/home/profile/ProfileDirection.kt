package com.grippo.home.profile

import com.grippo.core.models.BaseDirection

internal sealed interface ProfileDirection : BaseDirection {
    data object ExcludedMuscles : ProfileDirection
    data object MissingEquipment : ProfileDirection
    data object ExerciseLibrary : ProfileDirection
    data object WeightHistory : ProfileDirection
}