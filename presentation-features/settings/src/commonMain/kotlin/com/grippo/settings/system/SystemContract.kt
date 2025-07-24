package com.grippo.settings.system

internal interface SystemContract {
    fun back()

    companion object Empty : SystemContract {
        override fun back() {}
    }
}