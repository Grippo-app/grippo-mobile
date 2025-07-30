package com.grippo.settings.system

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.settings.SettingsFeature
import com.grippo.data.features.api.settings.models.Settings
import com.grippo.domain.state.settings.toState
import com.grippo.state.domain.settings.toDomain
import com.grippo.state.settings.ThemeState
import kotlinx.coroutines.flow.onEach

internal class SystemViewModel(
    private val settingsFeature: SettingsFeature
) : BaseViewModel<SystemState, SystemDirection, SystemLoader>(SystemState()),
    SystemContract {

    init {
        settingsFeature.observeSettings()
            .onEach(::provideSettings)
            .safeLaunch()
    }

    override fun onThemeClick(theme: ThemeState) {
        update { it.copy(theme = theme) }
        safeLaunch { settingsFeature.setTheme(mode = theme.toDomain()).getOrThrow() }
    }

    private fun provideSettings(value: Settings?) {
        val theme = value?.theme?.toState() ?: ThemeState.LIGHT
        update { it.copy(theme = theme) }
    }

    override fun back() {
        navigateTo(SystemDirection.Back)
    }
}