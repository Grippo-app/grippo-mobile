package com.grippo.training.exercise

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.PersistenceKey
import com.grippo.core.foundation.models.Result
import com.grippo.core.foundation.models.ResultKey
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.screen.api.ExercisesRouter
import com.grippo.screen.api.TrainingRouter

internal class TrainingExerciseComponent(
    componentContext: ComponentContext,
    exercise: ExerciseState,
    private val resultKey: ResultKey<Result<TrainingRouter.Exercise.Action>>,
    private val toLibrary: (ExercisesRouter.Library) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<TrainingExerciseDirection>(componentContext) {

    override val viewModel: TrainingExerciseViewModel = componentContext.retainedInstance {
        TrainingExerciseViewModel(
            exercise = exercise,
            analytics = getKoin().get(),
            exerciseExampleFeature = getKoin().get(),
            dialogController = getKoin().get(),
            weightHistoryFeature = getKoin().get(),
            exerciseValidatorUseCase = getKoin().get(),
            initialState = restoreState(DRAFT),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        saveState(DRAFT) { viewModel.state.value }

        backHandler.register(backCallback)

        observeResult(
            key = TrainingExerciseViewModel.libraryResultKey(exercise.id),
            onResult = { result ->
                viewModel.swapExerciseExample(result.data)
            },
        )
    }

    override suspend fun eventListener(direction: TrainingExerciseDirection) {
        when (direction) {
            is TrainingExerciseDirection.ToLibrary -> toLibrary(direction.route)
            TrainingExerciseDirection.Back -> back.invoke()

            is TrainingExerciseDirection.Update -> {
                val action = TrainingRouter.Exercise.Action.Sync(exercise = direction.exercise)

                sendResult(
                    key = resultKey,
                    data = action,
                )
            }

            is TrainingExerciseDirection.Delete -> {
                val action = TrainingRouter.Exercise.Action.Remove(id = direction.id)

                sendResult(
                    key = resultKey,
                    data = action,
                )
            }

            is TrainingExerciseDirection.Save -> {
                val action = TrainingRouter.Exercise.Action.Sync(exercise = direction.exercise)

                sendResult(
                    key = resultKey,
                    data = action,
                )
                back.invoke()
            }
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()

        TrainingExerciseScreen(
            state = state.value,
            contract = viewModel,
        )
    }

    private companion object {
        val DRAFT = PersistenceKey(
            key = "TrainingExerciseDraft",
            serializer = TrainingExerciseState.serializer(),
        )
    }
}
