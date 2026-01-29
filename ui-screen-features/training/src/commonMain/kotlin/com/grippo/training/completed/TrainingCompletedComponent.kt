package com.grippo.training.completed

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.datetime.LocalDateTime

internal class TrainingCompletedComponent(
    stage: StageState,
    componentContext: ComponentContext,
    exercises: List<ExerciseState>,
    startAt: LocalDateTime,
    private val back: () -> Unit,
) : BaseComponent<TrainingCompletedDirection>(componentContext) {

    override val viewModel: TrainingCompletedViewModel = componentContext.retainedInstance {
        TrainingCompletedViewModel(
            stage = stage,
            exercises = exercises,
            startAt = startAt,
            trainingFeature = getKoin().get(),
            trainingTotalUseCase = getKoin().get(),
            dialogController = getKoin().get(),
            exerciseExampleFeature = getKoin().get(),
            trainingTimelineUseCase = getKoin().get(),
            muscleLoadingSummaryUseCase = getKoin().get(),
            stringProvider = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingCompletedDirection) {
        when (direction) {
            TrainingCompletedDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingCompletedScreen(state.value, loaders.value, viewModel)
    }
}
