package com.grippo.home

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class BottomNavigationState(
    val segments: ImmutableList<Pair<Int, String>> = BottomBarMenu.entries.map {
        it.ordinal to it.name
    }.toPersistentList(),
    val selectedIndex: Int = 0,
)

@Immutable
internal enum class BottomBarMenu {
    Trainings,
    Profile,
}