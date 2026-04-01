package com.grippo.shared.root

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.stage.StageState
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.shared.deeplink.DeeplinkParser
import com.grippo.toolkit.connectivity.Connectivity
import kotlinx.coroutines.flow.onEach

public class RootViewModel(
    authorizationFeature: AuthorizationFeature,
    connectivity: Connectivity,
    deeplink: String? = null,
) : BaseViewModel<RootState, RootDirection, RootLoader>(RootState(deeplink = deeplink)),
    RootContract {

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

    private fun provideConnectionStatus(value: Connectivity.Status) {}

    override fun onClose() {
        navigateTo(RootDirection.Close)
    }

    override fun toHome() {
        navigateTo(RootDirection.Home)
        state.value.deeplink?.let { DeeplinkParser.parse(it)?.let { dir -> navigateTo(dir) } }
        update { it.copy(deeplink = null) }
    }

    /** Cold start — queue for consumption when [toHome] is called. */
    internal fun enqueueDeeplink(deeplink: String) {
        update { it.copy(deeplink = deeplink) }
    }

    /** Warm start — already on Home, apply immediately. */
    internal fun applyDeeplink(deeplink: String) {
        DeeplinkParser.parse(deeplink)?.let { navigateTo(it) }
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
