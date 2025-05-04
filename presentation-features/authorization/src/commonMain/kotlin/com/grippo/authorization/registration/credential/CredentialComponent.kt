package com.grippo.authorization.registration.credential

import com.arkivanov.decompose.ComponentContext
import androidx.compose.runtime.Composable
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.core.BaseComponent
import com.arkivanov.essenty.instancekeeper.retainedInstance

internal class CredentialComponent(
    componentContext: ComponentContext,
) : BaseComponent<CredentialDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        CredentialViewModel()
    }

    override suspend fun eventListener(rout: CredentialDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        CredentialScreen(state.value, loaders.value, viewModel)
    }
}