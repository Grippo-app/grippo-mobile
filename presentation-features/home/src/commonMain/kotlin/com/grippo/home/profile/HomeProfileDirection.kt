package com.grippo.home.profile

import com.grippo.core.models.BaseDirection

internal sealed interface HomeProfileDirection : BaseDirection {
    data object ExcludedMuscles : HomeProfileDirection
    data object MissingEquipment : HomeProfileDirection
    data object ExerciseLibrary : HomeProfileDirection
    data object WeightHistory : HomeProfileDirection
    data object Back : HomeProfileDirection
    data object Debug : HomeProfileDirection
    data object Workout : HomeProfileDirection
    data object SystemSettings : HomeProfileDirection
}