package com.grippo.shared.root

import com.grippo.connectivity.Connectivity
import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.settings.SettingsFeature
import com.grippo.data.features.api.settings.models.Settings
import com.grippo.design.components.connection.snackbar.ConnectionSnackbarState
import com.grippo.domain.state.settings.toState
import com.grippo.state.settings.ThemeState
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
            .safeLaunch()
    }

    private fun provideSettings(value: Settings?) {
        val theme = value?.theme?.toState() ?: ThemeState.LIGHT
        update { it.copy(theme = theme) }
    }

    private fun provideConnectionStatus(value: Connectivity.Status) {
        val connection = when (value) {
            is Connectivity.Status.Connected -> ConnectionSnackbarState.Hidden
            Connectivity.Status.Disconnected -> ConnectionSnackbarState.Visible
        }
        update { it.copy(connection = connection) }
    }

    override fun onClose() {
        navigateTo(RootDirection.Close)
    }

    override fun toHome() {
        navigateTo(RootDirection.ToHome)
    }

    override fun toProfile() {
        navigateTo(RootDirection.ToProfile)
    }

    override fun toDebug() {
        navigateTo(RootDirection.ToDebug)
    }

    override fun toSettings() {
        navigateTo(RootDirection.ToSettings)
    }

    override fun toTraining() {
        navigateTo(RootDirection.ToTraining)
    }

    override fun toWeightHistory() {
        navigateTo(RootDirection.ToWeightHistory)
    }

    override fun toMissingEquipment() {
        navigateTo(RootDirection.ToMissingEquipment)
    }

    override fun toExcludedMuscles() {
        navigateTo(RootDirection.ToExcludedMuscles)
    }

    override fun toSystemSettings() {
        navigateTo(RootDirection.ToSystemSettings)
    }

    override fun onBack() {
        navigateTo(RootDirection.Back)
    }
}