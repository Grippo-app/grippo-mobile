package com.grippo.authorization.registration.completed

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class CompletedComponent(
    componentContext: ComponentContext,
) : BaseComponent<CompletedDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        CompletedViewModel()
    }

    override suspend fun eventListener(direction: CompletedDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        CompletedScreen(state.value, loaders.value, viewModel)
    }
}