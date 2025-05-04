package com.grippo.authorization.registration

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class RegistrationComponent(
    componentContext: ComponentContext,
) : BaseComponent<RegistrationDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        RegistrationViewModel()
    }

    override suspend fun eventListener(rout: RegistrationDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        RegistrationScreen(state.value, loaders.value, viewModel)
    }
}