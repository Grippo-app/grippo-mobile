package com.grippo.authorization.login

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class LoginComponent(
    componentContext: ComponentContext,
    private val toRegistration: (email: String?) -> Unit,
    private val toHome: () -> Unit,
    private val toCreateProfile: () -> Unit,
    private val back: () -> Unit,
) : BaseComponent<LoginDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        LoginViewModel(
            loginUseCase = getKoin().get(),
            googleAuthProvider = getKoin().get(),
            appleAuthProvider = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: LoginDirection) {
        when (direction) {
            is LoginDirection.Registration -> toRegistration.invoke(direction.email)
            LoginDirection.Home -> toHome.invoke()
            LoginDirection.CreateProfile -> toCreateProfile.invoke()
            LoginDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        LoginScreen(state.value, loaders.value, viewModel)
    }
}
