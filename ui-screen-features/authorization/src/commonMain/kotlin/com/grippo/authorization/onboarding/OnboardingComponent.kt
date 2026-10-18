package com.grippo.authorization.onboarding

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class OnboardingComponent(
    componentContext: ComponentContext,
    private val toLogin: () -> Unit,
) : BaseComponent<OnboardingDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        OnboardingViewModel(
            analytics = getKoin().get(),
            localSettingsFeature = getKoin().get(),
        )
    }

    init {
        backHandler.register(BackCallback(onBack = viewModel::onBack))
    }

    override suspend fun eventListener(direction: OnboardingDirection) {
        when (direction) {
            OnboardingDirection.Login -> toLogin.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        OnboardingScreen(
            state = state.value,
            loaders = loaders.value,
            contract = viewModel,
        )
    }
}
