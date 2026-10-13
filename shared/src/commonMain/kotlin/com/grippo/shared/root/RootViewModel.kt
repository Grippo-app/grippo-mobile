package com.grippo.shared.root

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.stage.StageState
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.screen.api.deeplink.Deeplink
import kotlinx.coroutines.flow.onEach

public class RootViewModel(
    deeplink: String? = null,
    authorizationFeature: AuthorizationFeature,
) : BaseViewModel<RootState, RootDirection, RootLoader>(RootState(deeplink = deeplink)),
    RootContract {

    init {
        authorizationFeature
            .getToken()
            .onEach { if (it == null) navigateTo(RootDirection.Login) }
            .safeLaunch()
    }

    override fun onClose() {
        navigateTo(RootDirection.Close)
    }

    override fun toHome() {
        navigateTo(RootDirection.Home)
        val direction = state.value.deeplink?.let(::parseDeeplink)
        if (direction != null) {
            navigateTo(direction)
        }
        update { it.copy(deeplink = null) }
    }

    /** Cold start — queue for consumption when [toHome] is called. */
    internal fun enqueueDeeplink(deeplink: String) {
        update { it.copy(deeplink = deeplink) }
    }

    /** Warm start — already on Home, apply immediately. */
    internal fun applyDeeplink(deeplink: String) {
        parseDeeplink(deeplink)?.let { navigateTo(it) }
    }

    private fun parseDeeplink(raw: String): RootDirection? = when (Deeplink.fromKey(raw)) {
        Deeplink.TrainingDraft -> RootDirection.Training(StageState.Draft)
        Deeplink.WeightHistory -> RootDirection.WeightHistory
        null -> null
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

    override fun toGoal() {
        navigateTo(RootDirection.Goal)
    }

    override fun onBack() {
        navigateTo(RootDirection.Back)
    }
}
