package com.grippo.debug

internal interface DebugContract {
    fun onBack()
    fun onSelect(value: DebugMenu)
    fun onSelectLogCategory(value: String)

    companion object Empty : DebugContract {
        override fun onBack() {}
        override fun onSelect(value: DebugMenu) {}
        override fun onSelectLogCategory(value: String) {}
    }
}
