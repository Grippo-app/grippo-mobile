package com.grippo.authorization.registration.credential

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class CredentialComponent(
    componentContext: ComponentContext,
    private val toName: (email: String, password: String) -> Unit,
    private val onBack: () -> Unit,
) : BaseComponent<CredentialDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        CredentialViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::back)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: CredentialDirection) {
        when (direction) {
            is CredentialDirection.Name -> toName.invoke(direction.email, direction.password)
            CredentialDirection.Back -> onBack.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        CredentialScreen(state.value, loaders.value, viewModel)
    }
}