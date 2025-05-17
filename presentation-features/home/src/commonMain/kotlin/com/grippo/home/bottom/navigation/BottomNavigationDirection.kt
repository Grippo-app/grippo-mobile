package com.grippo.home.bottom.navigation

import com.grippo.core.models.BaseDirection

internal sealed interface BottomNavigationDirection : BaseDirection {
    data object Trainings : BottomNavigationDirection
    data object Profile : BottomNavigationDirection
}