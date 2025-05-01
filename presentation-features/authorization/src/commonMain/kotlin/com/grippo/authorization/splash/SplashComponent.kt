package com.grippo.authorization.splash

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent

internal class SplashComponent(
    componentContext: ComponentContext,
) : BaseComponent<SplashDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        SplashViewModel()
    }

    override suspend fun eventListener(rout: SplashDirection) {
    }

    @Composable
    override fun Render() {
    }
}