package com.grippo.exercise.examples.list

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform

internal class ExerciseExampleListComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<ExerciseExampleListDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ExerciseExampleListViewModel(
            exerciseExampleFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ExerciseExampleListDirection) {
        when (direction) {
            ExerciseExampleListDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExerciseExampleListScreen(state.value, loaders.value, viewModel)
    }
}
