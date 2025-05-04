package com.grippo.authorization.splash

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class SplashComponent(
    componentContext: ComponentContext,
    private val toAuthProcess: () -> Unit
) : BaseComponent<SplashDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        SplashViewModel()
    }

    override suspend fun eventListener(direction: SplashDirection) {
        when (direction) {
            SplashDirection.AuthProcess -> toAuthProcess.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        SplashScreen(state.value, loaders.value, viewModel)
    }
}