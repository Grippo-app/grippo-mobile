package com.grippo.exercise.example.exerciseexample

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.dialog.api.DialogConfig

public class ExerciseExampleDetailsComponent(
    componentContext: ComponentContext,
    id: String,
    mode: DialogConfig.ExerciseExampleDetails.Mode,
    private val onAction: () -> Unit,
    private val back: () -> Unit,
) : BaseComponent<ExerciseExampleDetailsDirection>(componentContext) {

    override val viewModel: ExerciseExampleDetailsViewModel = componentContext.retainedInstance {
        ExerciseExampleDetailsViewModel(
            analytics = getKoin().get(),
            id = id,
            mode = mode.toState(),
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

    override suspend fun eventListener(direction: ExerciseExampleDetailsDirection) {
        when (direction) {
            ExerciseExampleDetailsDirection.Back -> back.invoke()
            ExerciseExampleDetailsDirection.Action -> onAction.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExerciseExampleDetailsScreen(state.value, loaders.value, viewModel)
    }
}
