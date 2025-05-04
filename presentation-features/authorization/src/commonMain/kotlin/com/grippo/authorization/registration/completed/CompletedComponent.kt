package com.grippo.authorization.registration.completed

import com.arkivanov.decompose.ComponentContext
import androidx.compose.runtime.Composable
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.core.BaseComponent
import com.arkivanov.essenty.instancekeeper.retainedInstance

internal class CompletedComponent(
    componentContext: ComponentContext,
) : BaseComponent<CompletedDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        CompletedViewModel()
    }

    override suspend fun eventListener(rout: CompletedDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        CompletedScreen(state.value, loaders.value, viewModel)
    }
}