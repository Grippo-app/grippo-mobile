package com.grippo.authorization.registration.name

import com.arkivanov.decompose.ComponentContext
import androidx.compose.runtime.Composable
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.core.BaseComponent
import com.arkivanov.essenty.instancekeeper.retainedInstance

internal class NameComponent(
    componentContext: ComponentContext,
) : BaseComponent<NameDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        NameViewModel()
    }

    override suspend fun eventListener(rout: NameDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        NameScreen(state.value, loaders.value, viewModel)
    }
}