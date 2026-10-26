package com.grippo.training.completed

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.PersistenceKey
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.datetime.LocalDateTime

internal class TrainingCompletedComponent(
    stage: StageState,
    componentContext: ComponentContext,
    exercises: List<ExerciseState>,
    startAt: LocalDateTime,
    private val toGoal: () -> Unit,
    private val back: () -> Unit,
) : BaseComponent<TrainingCompletedDirection>(componentContext) {

    override val viewModel: TrainingCompletedViewModel = componentContext.retainedInstance {
        TrainingCompletedViewModel(
            stage = stage,
            exercises = exercises,
            startAt = startAt,
            analytics = getKoin().get(),
            trainingFeature = getKoin().get(),
            exerciseExampleFeature = getKoin().get(),
            localSettingsFeature = getKoin().get(),
            goalFeature = getKoin().get(),
            setTrainingUseCase = getKoin().get(),
            updateTrainingUseCase = getKoin().get(),
            refreshExerciseExamplesUseCase = getKoin().get(),
            trainingTotalUseCase = getKoin().get(),
            muscleLoadingSummaryUseCase = getKoin().get(),
            performanceTrendUseCase = getKoin().get(),
            trainingRecordsUseCase = getKoin().get(),
            weeklyVolumeUseCase = getKoin().get(),
            trainingStreakUseCase = getKoin().get(),
            goalFollowingUseCase = getKoin().get(),
            dialogController = getKoin().get(),
            imageSharer = getKoin().get(),
            initialState = restoreState(DRAFT) ?: TrainingCompletedState(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        saveState(DRAFT) {
            viewModel.state.value
        }

        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingCompletedDirection) {
        when (direction) {
            TrainingCompletedDirection.Back -> {
                back.invoke()
            }

            TrainingCompletedDirection.Goal -> {
                toGoal.invoke()
            }
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()

        TrainingCompletedScreen(
            state = state.value,
            loaders = loaders.value,
            contract = viewModel,
        )
    }

    private companion object {
        val DRAFT = PersistenceKey(
            key = "TrainingCompletedDraft",
            serializer = TrainingCompletedState.serializer(),
        )
    }
}
