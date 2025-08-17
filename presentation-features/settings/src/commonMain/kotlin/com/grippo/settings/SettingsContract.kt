package com.grippo.settings

internal interface SettingsContract {
    fun onBack()

    companion object Empty : SettingsContract {
        override fun onBack() {}
    }
}