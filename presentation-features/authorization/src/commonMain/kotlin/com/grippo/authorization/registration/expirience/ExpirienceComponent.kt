package com.grippo.authorization.registration.expirience

import com.arkivanov.decompose.ComponentContext
import androidx.compose.runtime.Composable
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.core.BaseComponent
import com.arkivanov.essenty.instancekeeper.retainedInstance

internal class ExpirienceComponent(
    componentContext: ComponentContext,
) : BaseComponent<ExpirienceDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ExpirienceViewModel()
    }

    override suspend fun eventListener(rout: ExpirienceDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExpirienceScreen(state.value, loaders.value, viewModel)
    }
}