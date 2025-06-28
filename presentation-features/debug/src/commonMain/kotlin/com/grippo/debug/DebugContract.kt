package com.grippo.debug

internal interface DebugContract {
    fun back()
    fun select(value: DebugMenu)

    companion object Empty : DebugContract {
        override fun back() {}
        override fun select(value: DebugMenu) {}
    }
}