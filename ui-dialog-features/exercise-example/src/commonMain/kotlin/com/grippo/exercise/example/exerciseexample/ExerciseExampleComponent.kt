package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

public class ExerciseExampleComponent(
    componentContext: ComponentContext,
    id: String,
    private val back: () -> Unit,
) : BaseComponent<ExerciseExampleDirection>(componentContext) {

    override val viewModel: ExerciseExampleViewModel = componentContext.retainedInstance {
        ExerciseExampleViewModel(
            id = id,
            exerciseExampleFeature = getKoin().get(),
            exerciseMetricsFeature = getKoin().get(),
            muscleLoadingSummaryUseCase = getKoin().get(),
            volumeSeriesUseCase = getKoin().get(),
            estimatedOneRepMaxUseCase = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ExerciseExampleDirection) {
        when (direction) {
            ExerciseExampleDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExerciseExampleScreen(state.value, loaders.value, viewModel)
    }
}
