package com.grippo.debug

import androidx.compose.runtime.Immutable

@Immutable
public data class DebugState(
    val selected: DebugMenu = DebugMenu.General
)

public enum class DebugMenu {
    General,
    Logger
}