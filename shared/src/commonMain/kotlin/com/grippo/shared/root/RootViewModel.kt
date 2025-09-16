package com.grippo.shared.root

import com.grippo.connectivity.Connectivity
import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.design.components.connection.snackbar.ConnectionSnackbarState
import kotlinx.coroutines.flow.onEach

public class RootViewModel(
    authorizationFeature: AuthorizationFeature,
    connectivity: Connectivity,
) : BaseViewModel<RootState, RootDirection, RootLoader>(RootState()), RootContract {

    init {
        authorizationFeature
            .getToken()
            .onEach { if (it == null) navigateTo(RootDirection.Login) }
            .safeLaunch()

        connectivity
            .statusUpdates
            .onEach(::provideConnectionStatus)
            .safeLaunch()
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

    override fun onBack() {
        navigateTo(RootDirection.Back)
    }
}