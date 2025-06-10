package com.grippo.home

import com.grippo.core.models.BaseDirection

public sealed interface BottomNavigationDirection : BaseDirection {
    public data object Trainings : BottomNavigationDirection
    public data object Statistics : BottomNavigationDirection
    public data object Profile : BottomNavigationDirection
    public data object Back : BottomNavigationDirection
}