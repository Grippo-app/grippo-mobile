package com.grippo.home

import androidx.compose.runtime.Immutable

@Immutable
public data object BottomNavigationState

@Immutable
internal enum class BottomBarMenu {
    Trainings,
    Profile,
}