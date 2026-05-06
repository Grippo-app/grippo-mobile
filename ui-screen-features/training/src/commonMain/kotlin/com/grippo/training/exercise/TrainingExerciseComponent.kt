package com.grippo.training.exercise

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.ResultKeys
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.trainings.ExerciseState

internal class TrainingExerciseComponent(
    componentContext: ComponentContext,
    private val exercise: ExerciseState,
    private val back: () -> Unit,
) : BaseComponent<TrainingExerciseDirection>(componentContext) {

    override val viewModel: TrainingExerciseViewModel = componentContext.retainedInstance {
        TrainingExerciseViewModel(
            exercise = exercise,
            exerciseExampleFeature = getKoin().get(),
            trainingTotalUseCase = getKoin().get(),
            dialogController = getKoin().get(),
            weightHistoryFeature = getKoin().get(),
            exerciseValidatorUseCase = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingExerciseDirection) {
        when (direction) {
            TrainingExerciseDirection.Back -> back.invoke()

            is TrainingExerciseDirection.Update ->
                sendResult(ResultKeys.create("exercise"), direction.exercise)

            is TrainingExerciseDirection.Save -> {
                sendResult(ResultKeys.create("exercise"), direction.exercise)
                back.invoke()
            }
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingExerciseScreen(state.value, loaders.value, viewModel)
    }
}
