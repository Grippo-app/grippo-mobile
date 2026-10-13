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
    private val exercise: ExerciseState,
    private val resultKey: ResultKey<Result<TrainingRouter.Exercise.Action>>,
    private val toLibrary: (ExercisesRouter.Library) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<TrainingExerciseDirection>(componentContext) {

    override val viewModel: TrainingExerciseViewModel = componentContext.retainedInstance {
        TrainingExerciseViewModel(
            analytics = getKoin().get(),
            initialState = restoreState(DRAFT),
            exercise = exercise,
            exerciseExampleFeature = getKoin().get(),
            dialogController = getKoin().get(),
            weightHistoryFeature = getKoin().get(),
            exerciseValidatorUseCase = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        saveState(DRAFT) { viewModel.state.value }
        backHandler.register(backCallback)

        observeResult(
            key = TrainingExerciseViewModel.libraryResultKey(exercise.id),
            onResult = { viewModel.swapExerciseExample(it.data) },
        )
    }

    override suspend fun eventListener(direction: TrainingExerciseDirection) {
        when (direction) {
            is TrainingExerciseDirection.ToLibrary -> toLibrary(direction.route)
            TrainingExerciseDirection.Back -> back.invoke()

            is TrainingExerciseDirection.Update ->
                sendResult(
                    resultKey,
                    TrainingRouter.Exercise.Action.Sync(direction.exercise)
                )

            is TrainingExerciseDirection.Delete ->
                sendResult(
                    resultKey,
                    TrainingRouter.Exercise.Action.Remove(direction.id)
                )

            is TrainingExerciseDirection.Save -> {
                sendResult(
                    resultKey,
                    TrainingRouter.Exercise.Action.Sync(direction.exercise)
                )
                back.invoke()
            }
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        TrainingExerciseScreen(state.value, viewModel)
    }

    private companion object {
        val DRAFT = PersistenceKey("TrainingExerciseDraft", TrainingExerciseState.serializer())
    }
}
