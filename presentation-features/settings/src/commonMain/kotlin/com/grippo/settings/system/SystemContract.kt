package com.grippo.settings.system

import com.grippo.state.settings.ThemeState

internal interface SystemContract {
    fun onThemeClick(theme: ThemeState)
    fun back()

    companion object Empty : SystemContract {
        override fun onThemeClick(theme: ThemeState) {}
        override fun back() {}
    }
}