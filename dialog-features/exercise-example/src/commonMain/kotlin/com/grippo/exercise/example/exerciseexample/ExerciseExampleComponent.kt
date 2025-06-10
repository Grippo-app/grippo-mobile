package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

public class ExerciseExampleComponent(
    componentContext: ComponentContext,
    id: String,
    private val onResult: (id: String) -> Unit,
    private val onDismiss: () -> Unit,
) : BaseComponent<ExerciseExampleDirection>(componentContext) {

    override val viewModel: ExerciseExampleViewModel = componentContext.retainedInstance {
        ExerciseExampleViewModel(
            id = id,
            exerciseExampleFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::dismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ExerciseExampleDirection) {
        when (direction) {
            is ExerciseExampleDirection.DismissWithResult -> onResult.invoke(direction.id)
            ExerciseExampleDirection.Dismiss -> onDismiss.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExerciseExampleScreen(state.value, loaders.value, viewModel)
    }
}