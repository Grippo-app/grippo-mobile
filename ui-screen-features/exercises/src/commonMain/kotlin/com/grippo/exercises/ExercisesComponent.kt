package com.grippo.exercises

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

public class ExercisesComponent(
    componentContext: ComponentContext,
) : BaseComponent<ExercisesDirection>(componentContext) {

    override val viewModel: ExercisesViewModel = componentContext.retainedInstance {
        ExercisesViewModel(
            exerciseExampleFeature = getKoin().get(),
            dialogController = getKoin().get(),
        )
    }

    override suspend fun eventListener(direction: ExercisesDirection) {
        // No off-tab navigation: taps open a dialog, not a route.
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExercisesScreen(state.value, loaders.value, viewModel)
    }
}
