package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

internal class ExerciseExampleComponent(
    componentContext: ComponentContext,
) : BaseComponent<ExerciseExampleDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ExerciseExampleViewModel()
    }

    override suspend fun eventListener(direction: ExerciseExampleDirection) {
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExerciseExampleScreen(state.value, loaders.value, viewModel)
    }
}