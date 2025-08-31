package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.state.exercise.examples.ExerciseExampleDialogMode
import com.grippo.state.exercise.examples.ExerciseExampleState

public class ExerciseExampleComponent(
    componentContext: ComponentContext,
    id: String,
    mode: ExerciseExampleDialogMode,
    private val back: () -> Unit,
    private val onResult: (value: ExerciseExampleState) -> Unit,
) : BaseComponent<ExerciseExampleDirection>(componentContext) {

    override val viewModel: ExerciseExampleViewModel = componentContext.retainedInstance {
        ExerciseExampleViewModel(
            id = id,
            mode = mode,
            exerciseExampleFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ExerciseExampleDirection) {
        when (direction) {
            ExerciseExampleDirection.Back -> back.invoke()
            is ExerciseExampleDirection.BackWithResult -> onResult.invoke(direction.value)
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExerciseExampleScreen(state.value, loaders.value, viewModel)
    }
}