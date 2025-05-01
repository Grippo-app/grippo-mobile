package com.grippo.authorization.login

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent

internal class LoginComponent(
    componentContext: ComponentContext,
) : BaseComponent<LoginDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        LoginViewModel()
    }

    override suspend fun eventListener(rout: LoginDirection) {
    }

    @Composable
    override fun Render() {
        LoginScreen()
    }
}