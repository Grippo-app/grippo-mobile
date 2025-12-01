package com.grippo.authorization.registration.credential

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class CredentialComponent(
    componentContext: ComponentContext,
    private val toCreateProfile: () -> Unit,
    private val toHome: () -> Unit,
    private val back: () -> Unit,
) : BaseComponent<CredentialDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        CredentialViewModel(
            registerUseCase = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: CredentialDirection) {
        when (direction) {
            CredentialDirection.CreateProfile -> toCreateProfile.invoke()
            CredentialDirection.Home -> toHome.invoke()
            CredentialDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        CredentialScreen(state.value, loaders.value, viewModel)
    }
}
