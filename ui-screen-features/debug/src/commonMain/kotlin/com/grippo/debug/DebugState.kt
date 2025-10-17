package com.grippo.debug

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

@Immutable
public data class DebugState(
    val selected: DebugMenu = DebugMenu.Logger,
    val logger: LoggerState = LoggerState()
)

@Immutable
public data class LoggerState(
    val categories: PersistentList<String> = persistentListOf(),
    val selectedCategory: String? = null,
    val logsByCategory: PersistentMap<String, PersistentList<String>> = persistentMapOf()
) {
    public val logs: PersistentList<String>
        get() = logsByCategory[selectedCategory] ?: persistentListOf()
}

@Immutable
public enum class DebugMenu {
    Logger,
    General,
}
