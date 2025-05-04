package com.grippo.authorization.registration.excluded.muscles

import com.arkivanov.decompose.ComponentContext
import androidx.compose.runtime.Composable
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.core.BaseComponent
import com.arkivanov.essenty.instancekeeper.retainedInstance

internal class ExcludedMusclesComponent(
    componentContext: ComponentContext,
) : BaseComponent<ExcludedMusclesDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ExcludedMusclesViewModel()
    }

    override suspend fun eventListener(rout: ExcludedMusclesDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExcludedMusclesScreen(state.value, loaders.value, viewModel)
    }
}