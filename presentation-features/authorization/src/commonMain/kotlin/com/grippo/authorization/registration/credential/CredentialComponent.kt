package com.grippo.authorization.registration.credential

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class CredentialComponent(
    componentContext: ComponentContext,
) : BaseComponent<CredentialDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        CredentialViewModel()
    }

    override suspend fun eventListener(direction: CredentialDirection) {
        when (direction) {
            CredentialDirection.Name -> TODO()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        CredentialScreen(state.value, loaders.value, viewModel)
    }
}