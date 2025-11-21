package com.grippo.debug

import androidx.compose.runtime.Immutable

@Immutable
internal interface DebugContract {
    fun onBack()
    fun clearLogs()
    fun generateTraining()
    fun onSelect(value: DebugMenu)
    fun onSelectLogCategory(value: String)

    @Immutable
    companion object Empty : DebugContract {
        override fun onBack() {}
        override fun clearLogs() {}
        override fun generateTraining() {}
        override fun onSelect(value: DebugMenu) {}
        override fun onSelectLogCategory(value: String) {}
    }
}
