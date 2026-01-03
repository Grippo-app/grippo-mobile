package com.grippo.training.recording

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.Result
import com.grippo.core.foundation.models.ResultKeys
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.datetime.LocalDateTime

internal class TrainingRecordingComponent(
    componentContext: ComponentContext,
    stage: StageState,
    private val toCompleted: (stage: StageState, exercises: List<ExerciseState>, startAt: LocalDateTime) -> Unit,
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
            stage = stage,
            exerciseExampleFeature = getKoin().get(),
            muscleFeature = getKoin().get(),
            trainingFeature = getKoin().get(),
            dialogController = getKoin().get(),
            stringProvider = getKoin().get(),
            trainingTotalUseCase = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingRecordingDirection) {
        when (direction) {
            is TrainingRecordingDirection.ToCompleted -> toCompleted.invoke(
                direction.stage,
                direction.exercises,
                direction.startAt
            )

            is TrainingRecordingDirection.ToExercise -> toExercise.invoke(
                direction.exercise
            )

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
