package com.grippo.authorization.splash

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class SplashComponent(
    componentContext: ComponentContext,
    private val toOnboarding: () -> Unit,
    private val toAuthProcess: () -> Unit,
    private val toHome: () -> Unit,
    private val toProfileCreation: () -> Unit,
    private val back: () -> Unit
) : BaseComponent<SplashDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        SplashViewModel(
            analytics = getKoin().get(),
            authorizationFeature = getKoin().get(),
            localSettingsFeature = getKoin().get(),
            userFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: SplashDirection) {
        when (direction) {
            SplashDirection.Onboarding -> toOnboarding.invoke()
            SplashDirection.AuthProcess -> toAuthProcess.invoke()
            SplashDirection.Home -> toHome.invoke()
            SplashDirection.ProfileCreation -> toProfileCreation.invoke()
            SplashDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        SplashScreen(state.value, loaders.value, viewModel)
    }
}
