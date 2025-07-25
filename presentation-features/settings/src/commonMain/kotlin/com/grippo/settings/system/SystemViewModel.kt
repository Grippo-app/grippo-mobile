package com.grippo.settings.system

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.settings.SettingsFeature
import com.grippo.data.features.api.settings.models.Settings
import com.grippo.domain.mapper.settings.toState
import kotlinx.coroutines.flow.onEach

internal class SystemViewModel(
    settingsFeature: SettingsFeature
) : BaseViewModel<SystemState, SystemDirection, SystemLoader>(SystemState()),
    SystemContract {

    init {
        settingsFeature.observeSettings()
            .onEach(::provideSettings)
            .safeLaunch()
    }

    private fun provideSettings(value: Settings?) {
        val colorMode = value?.colorMode?.toState() ?: return
        update { it.copy(colorMode = colorMode) }
    }

    override fun back() {
        navigateTo(SystemDirection.Back)
    }
}