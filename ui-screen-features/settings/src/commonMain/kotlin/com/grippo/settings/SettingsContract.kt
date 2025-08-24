package com.grippo.settings

internal interface SettingsContract {
    fun onClose()

    companion object Empty : SettingsContract {
        override fun onClose() {}
    }
}