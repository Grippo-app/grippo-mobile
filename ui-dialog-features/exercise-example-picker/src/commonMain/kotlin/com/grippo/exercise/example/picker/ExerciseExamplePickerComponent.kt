package com.grippo.exercise.example.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.state.exercise.examples.ExerciseExampleState

public class ExerciseExamplePickerComponent(
    componentContext: ComponentContext,
    private val onResult: (value: ExerciseExampleState) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<ExerciseExamplePickerDirection>(componentContext) {

    override val viewModel: ExerciseExamplePickerViewModel = componentContext.retainedInstance {
        ExerciseExamplePickerViewModel(
            exerciseExampleFeature = getKoin().get(),
            muscleFeature = getKoin().get(),
            dialogController = getKoin().get(),
            stringProvider = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ExerciseExamplePickerDirection) {
        when (direction) {
            is ExerciseExamplePickerDirection.BackWithResult -> onResult.invoke(direction.value)
            ExerciseExamplePickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExerciseExamplePickerScreen(state.value, loaders.value, viewModel)
    }
}