package com.grippo.home.trainings

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class HomeTrainingsComponent(
    componentContext: ComponentContext,
) : BaseComponent<HomeTrainingsDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        HomeTrainingsViewModel(
            trainingFeature = getKoin().get(),
            dialogController = getKoin().get()
        )
    }

    override suspend fun eventListener(direction: HomeTrainingsDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        HomeTrainingsScreen(state.value, loaders.value, viewModel)
    }
}