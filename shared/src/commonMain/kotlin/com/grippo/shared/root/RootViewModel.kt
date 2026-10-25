package com.grippo.shared.root

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.stage.StageState
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.screen.api.ExercisesRouter
import com.grippo.screen.api.deeplink.Deeplink
import kotlinx.coroutines.flow.onEach

public class RootViewModel(
    deeplink: String? = null,
    private val authorizationFeature: AuthorizationFeature,
) : BaseViewModel<RootState, RootDirection, RootLoader>(RootState(deeplink = deeplink)),
    RootContract {

    init {
        observeToken()
    }

    private fun observeToken() {
        authorizationFeature.getToken()
            .onEach(::provideToken)
            .safeLaunch(onError = ::observeToken)
    }

    private fun provideToken(value: String?) {
        if (value == null) {
            navigateTo(RootDirection.Login)
        }
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

    internal fun enqueueDeeplink(deeplink: String) {
        update { it.copy(deeplink = deeplink) }
    }

    internal fun applyDeeplink(deeplink: String) {
        parseDeeplink(deeplink)?.let { navigateTo(it) }
    }

    private fun parseDeeplink(raw: String): RootDirection? = when (Deeplink.fromKey(raw)) {
        Deeplink.TrainingDraft -> RootDirection.Training(StageState.Draft)
        Deeplink.WeightHistory -> RootDirection.WeightHistory
        null -> null
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

    override fun toExercises(route: ExercisesRouter.Library) {
        navigateTo(RootDirection.Exercises(route))
    }

    override fun toGoal() {
        navigateTo(RootDirection.Goal)
    }

    override fun onBack() {
        navigateTo(RootDirection.Back)
    }
}
