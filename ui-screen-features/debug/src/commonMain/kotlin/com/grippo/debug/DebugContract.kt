package com.grippo.debug

internal interface DebugContract {
    fun onBack()
    fun clearLogs()
    fun onSelect(value: DebugMenu)
    fun onSelectLogCategory(value: String)

    companion object Empty : DebugContract {
        override fun onBack() {}
        override fun clearLogs() {}
        override fun onSelect(value: DebugMenu) {}
        override fun onSelectLogCategory(value: String) {}
    }
}
