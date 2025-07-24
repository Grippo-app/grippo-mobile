package com.grippo.settings

import com.grippo.core.BaseViewModel

internal class SettingsViewModel :
    BaseViewModel<SettingsState, SettingsDirection, SettingsLoader>(SettingsState),
    SettingsContract {

    override fun back() {
        navigateTo(SettingsDirection.Back)
    }
}