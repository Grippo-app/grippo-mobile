package com.grippo.debug

internal interface DebugContract {
    fun onBack()
    fun onSelect(value: DebugMenu)

    companion object Empty : DebugContract {
        override fun onBack() {}
        override fun onSelect(value: DebugMenu) {}
    }
}