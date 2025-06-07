package com.grippo.authorization.login

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class LoginComponent(
    componentContext: ComponentContext,
    private val toRegistration: () -> Unit,
    private val toHome: () -> Unit,
) : BaseComponent<LoginDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        LoginViewModel(
            loginUseCase = getKoin().get()
        )
    }

    override suspend fun eventListener(direction: LoginDirection) {
        when (direction) {
            LoginDirection.Registration -> toRegistration.invoke()
            LoginDirection.Home -> toHome.invoke()
        }
    }

    @Composable
    override fun Render() {
        viewModel.state.collectAsStateMultiplatform()
        viewModel.loaders.collectAsStateMultiplatform()
//        LoginScreen(state.value, loaders.value, viewModel)
    }
}