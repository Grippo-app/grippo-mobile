package com.grippo.training.recording

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.models.Result
import com.grippo.core.models.ResultKeys
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState

internal class TrainingRecordingComponent(
    componentContext: ComponentContext,
    private val toCompleted: (training: TrainingState) -> Unit,
    private val toExercise: (exercise: ExerciseState) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<TrainingRecordingDirection>(componentContext) {

    init {
        observeResult<Result<ExerciseState>>(
            key = ResultKeys.create("exercise"),
            onResult = { viewModel.updateExercise(it.data) }
        )
    }

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
            is TrainingRecordingDirection.ToCompleted -> toCompleted.invoke(direction.training)
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