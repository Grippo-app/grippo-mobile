package com.grippo.debug

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Immutable
@Serializable
public data class DebugState(
    val selected: DebugMenu = DebugMenu.Logger,
    val logger: LoggerState = LoggerState(),
    @Transient
    val pushToken: String? = null
)

@Immutable
@Serializable
public data class LoggerState(
    @Transient
    val categories: PersistentList<String> = persistentListOf(),
    val selectedCategory: String? = null,
    @Transient
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
