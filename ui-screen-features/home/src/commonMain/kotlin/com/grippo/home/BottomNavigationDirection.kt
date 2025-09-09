package com.grippo.home

import com.grippo.core.models.BaseDirection

public sealed interface BottomNavigationDirection : BaseDirection {
    public data object Trainings : BottomNavigationDirection
    public data object Statistics : BottomNavigationDirection
    public data object Profile : BottomNavigationDirection
    public data object Back : BottomNavigationDirection
    public data object ToExcludedMuscles : BottomNavigationDirection
    public data object ToMissingEquipment : BottomNavigationDirection
    public data object ToWeightHistory : BottomNavigationDirection
    public data object ToDebug : BottomNavigationDirection
    public data object ToTraining : BottomNavigationDirection
    public data object ToSystemSettings : BottomNavigationDirection
}