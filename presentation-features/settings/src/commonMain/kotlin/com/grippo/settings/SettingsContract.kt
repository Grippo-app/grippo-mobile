package com.grippo.settings

internal interface SettingsContract {
    fun back()

    companion object Empty : SettingsContract {
        override fun back() {}
    }
}