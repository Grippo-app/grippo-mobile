package com.grippo.home.trainings

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class TrainingsComponent(
    componentContext: ComponentContext,
) : BaseComponent<TrainingsDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        TrainingsViewModel(
            trainingFeature = getKoin().get()
        )
    }

    override suspend fun eventListener(direction: TrainingsDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingsScreen(state.value, loaders.value, viewModel)
    }
}