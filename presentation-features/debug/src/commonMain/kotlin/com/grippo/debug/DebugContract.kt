package com.grippo.debug

internal interface DebugContract {
    fun back()

    companion object Empty : DebugContract {
        override fun back() {}
    }
}