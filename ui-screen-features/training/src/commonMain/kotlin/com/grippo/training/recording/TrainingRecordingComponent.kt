package com.grippo.training.recording

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.PersistenceKey
import com.grippo.core.foundation.models.Result
import com.grippo.core.foundation.models.ResultKey
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.screen.api.ExercisesRouter
import com.grippo.screen.api.TrainingRouter
import kotlinx.datetime.LocalDateTime

internal class TrainingRecordingComponent(
    componentContext: ComponentContext,
    stage: StageState,
    private val toCompleted: (
        stage: StageState,
        exercises: List<ExerciseState>,
        startAt: LocalDateTime,
    ) -> Unit,
    private val toExercise: (
        exercise: ExerciseState,
        resultKey: ResultKey<Result<TrainingRouter.Exercise.Action>>,
    ) -> Unit,
    private val toLibrary: (ExercisesRouter.Library) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<TrainingRecordingDirection>(componentContext) {

    init {
        observeResult(
            key = TrainingRecordingViewModel.LIBRARY_RESULT_KEY,
            onResult = { result ->
                viewModel.acceptLibrarySelection(result.data)
            },
        )

        observeResult(
            key = TrainingRecordingViewModel.EXERCISE_RESULT_KEY,
            onResult = { result ->
                when (val action = result.data) {
                    is TrainingRouter.Exercise.Action.Sync -> {
                        viewModel.updateExercise(action.exercise)
                    }

                    is TrainingRouter.Exercise.Action.Remove -> {
                        viewModel.removeExercise(action.id)
                    }
                }
            },
        )
    }

    override val viewModel: TrainingRecordingViewModel = componentContext.retainedInstance {
        TrainingRecordingViewModel(
            stage = stage,
            analytics = getKoin().get(),
            muscleFeature = getKoin().get(),
            exerciseExampleFeature = getKoin().get(),
            trainingFeature = getKoin().get(),
            dialogController = getKoin().get(),
            stringProvider = getKoin().get(),
            notificationManager = getKoin().get(),
            trainingTotalUseCase = getKoin().get(),
            initialState = restoreState(DRAFT),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        saveState(DRAFT) { viewModel.state.value }

        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingRecordingDirection) {
        when (direction) {
            is TrainingRecordingDirection.ToLibrary -> {
                toLibrary(direction.route)
            }

            is TrainingRecordingDirection.ToCompleted -> {
                toCompleted.invoke(
                    direction.stage,
                    direction.exercises,
                    direction.startAt,
                )
            }

            is TrainingRecordingDirection.ToExercise -> {
                toExercise.invoke(
                    direction.exercise,
                    direction.resultKey,
                )
            }

            TrainingRecordingDirection.Back -> {
                back.invoke()
            }
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()

        TrainingRecordingScreen(
            state = state.value,
            loaders = loaders.value,
            contract = viewModel,
        )
    }

    private companion object {
        val DRAFT = PersistenceKey(
            key = "TrainingRecordingDraft",
            serializer = TrainingRecordingState.serializer(),
        )
    }
}
