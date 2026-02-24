package com.grippo.shared.root

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.stage.StageState
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.design.components.connection.snackbar.ConnectionSnackbarState
import com.grippo.toolkit.connectivity.Connectivity
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
        when (value) {
            is Connectivity.Status.Connected -> ConnectionSnackbarState.Hidden
            Connectivity.Status.Disconnected -> ConnectionSnackbarState.Visible
        }
        // update { it.copy(connection = connection) }
    }

    override fun onClose() {
        navigateTo(RootDirection.Close)
    }

    override fun toHome() {
        navigateTo(RootDirection.Home)
    }

    override fun toProfile() {
        navigateTo(RootDirection.Profile)
    }

    override fun toDebug() {
        navigateTo(RootDirection.Debug)
    }

    override fun toTrainings() {
        navigateTo(RootDirection.Trainings)
    }

    override fun toTraining(stage: StageState) {
        navigateTo(RootDirection.Training(stage))
    }

    override fun toWeightHistory() {
        navigateTo(RootDirection.WeightHistory)
    }

    override fun toMissingEquipment() {
        navigateTo(RootDirection.MissingEquipment)
    }

    override fun toExcludedMuscles() {
        navigateTo(RootDirection.ExcludedMuscles)
    }

    override fun toExperience() {
        navigateTo(RootDirection.Experience)
    }

    override fun toSettings() {
        navigateTo(RootDirection.Settings)
    }

    override fun toSocial() {
        navigateTo(RootDirection.Social)
    }

    override fun onBack() {
        navigateTo(RootDirection.Back)
    }
}
