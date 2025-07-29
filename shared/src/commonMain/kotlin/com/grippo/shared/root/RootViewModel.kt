package com.grippo.shared.root

import com.grippo.connectivity.Connectivity
import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.settings.SettingsFeature
import com.grippo.data.features.api.settings.models.Settings
import com.grippo.domain.mapper.settings.toState
import com.grippo.state.settings.ThemeState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

public class RootViewModel(
    authorizationFeature: AuthorizationFeature,
    settingsFeature: SettingsFeature,
    connectivity: Connectivity,
) : BaseViewModel<RootState, RootDirection, RootLoader>(RootState()), RootContract {

    init {
        authorizationFeature
            .getToken()
            .onEach { if (it == null) navigateTo(RootDirection.Login) }
            .safeLaunch()

        settingsFeature
            .observeSettings()
            .onEach(::provideSettings)
            .safeLaunch()

        connectivity
            .statusUpdates
            .onEach(::provideConnectionStatus)
            .launchIn(coroutineScope)
    }

    private fun provideSettings(value: Settings?) {
        val theme = value?.theme?.toState() ?: ThemeState.LIGHT
        update { it.copy(theme = theme) }
    }

    private fun provideConnectionStatus(value: Connectivity.Status) {
        val isConnected = when (value) {
            is Connectivity.Status.Connected -> true
            Connectivity.Status.Disconnected -> false
        }
        update { it.copy(isConnectedToInternet = isConnected) }
    }

    override fun back() {
        navigateTo(RootDirection.Back)
    }
}