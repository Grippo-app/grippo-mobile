package com.grippo.settings

import com.grippo.core.BaseViewModel

public class SettingsViewModel : BaseViewModel<SettingsState, SettingsDirection, SettingsLoader>(
    SettingsState
), SettingsContract {

    override fun back() {
        navigateTo(SettingsDirection.Back)
    }
}