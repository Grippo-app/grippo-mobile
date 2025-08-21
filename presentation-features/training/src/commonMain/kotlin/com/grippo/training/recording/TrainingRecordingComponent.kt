package com.grippo.training.recording

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.state.trainings.ExerciseState

internal class TrainingRecordingComponent(
    componentContext: ComponentContext,
    private val toSuccess: () -> Unit,
    private val toExercise: (exercise: ExerciseState) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<TrainingRecordingDirection>(componentContext) {

    override val viewModel: TrainingRecordingViewModel = componentContext.retainedInstance {
        TrainingRecordingViewModel(
            dialogController = getKoin().get(),
            stringProvider = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingRecordingDirection) {
        when (direction) {
            TrainingRecordingDirection.ToSuccess -> toSuccess.invoke()
            is TrainingRecordingDirection.ToExercise -> toExercise.invoke(direction.exercise)
            TrainingRecordingDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingRecordingScreen(state.value, loaders.value, viewModel)
    }
}