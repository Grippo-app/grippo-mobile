package com.grippo.authorization.registration.name

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class NameComponent(
    componentContext: ComponentContext,
    private val toBody: () -> Unit
) : BaseComponent<NameDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        NameViewModel()
    }

    override suspend fun eventListener(direction: NameDirection) {
        when (direction) {
            NameDirection.Body -> toBody.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        NameScreen(state.value, loaders.value, viewModel)
    }
}