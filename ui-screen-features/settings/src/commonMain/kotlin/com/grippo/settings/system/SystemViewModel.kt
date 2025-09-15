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

    private fun provideSettings(value: Settings?) {
        value ?: return
        val theme = value.theme.toState()
        val locale = value.locale.toState()
        update { it.copy(theme = theme, locale = locale) }
    }

    override fun onThemeClick(theme: ThemeState) {
        update { it.copy(theme = theme) }
        safeLaunch { settingsFeature.setTheme(mode = theme.toDomain()).getOrThrow() }
    }

    override fun onLocaleClick() {
    }

    override fun onBack() {
        navigateTo(SystemDirection.Back)
    }
}