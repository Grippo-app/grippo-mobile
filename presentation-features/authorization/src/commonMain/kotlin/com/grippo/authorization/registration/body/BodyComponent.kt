package com.grippo.authorization.registration.body

import com.arkivanov.decompose.ComponentContext
import androidx.compose.runtime.Composable
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.core.BaseComponent
import com.arkivanov.essenty.instancekeeper.retainedInstance

internal class BodyComponent(
    componentContext: ComponentContext,
) : BaseComponent<BodyDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        BodyViewModel()
    }

    override suspend fun eventListener(rout: BodyDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        BodyScreen(state.value, loaders.value, viewModel)
    }
}