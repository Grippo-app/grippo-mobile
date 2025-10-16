package com.grippo.debug

import androidx.compose.runtime.Immutable

@Immutable
public data class DebugState(
    val selected: DebugMenu = DebugMenu.Logger
)

@Immutable
public enum class DebugMenu {
    Logger,
    General,
}