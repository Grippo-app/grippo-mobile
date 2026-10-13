package com.grippo.training.completed

import com.grippo.analytics.training.TrainingAnalytics
import com.grippo.analytics.training.models.TrainingEvent
import com.grippo.analytics.training.models.TrainingOperation
import com.grippo.analytics.training.models.TrainingScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.metrics.distribution.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.volume.TrainingTotalUseCase
import com.grippo.data.features.api.training.SetTrainingUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.TrainingTimelineUseCase
import com.grippo.data.features.api.training.UpdateTrainingUseCase
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_record
import com.grippo.design.resources.provider.statistics
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.metrics.distribution.toState
import com.grippo.domain.state.metrics.volume.toState
import com.grippo.domain.state.training.toState
import com.grippo.state.domain.training.toDomain
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.LocalDateTime

internal class TrainingCompletedViewModel(
    stage: StageState,
    exercises: List<ExerciseState>,
    startAt: LocalDateTime,
    private val analytics: TrainingAnalytics,
    private val trainingFeature: TrainingFeature,
    setTrainingUseCase: SetTrainingUseCase,
    updateTrainingUseCase: UpdateTrainingUseCase,
    trainingTotalUseCase: TrainingTotalUseCase,
    private val dialogController: DialogController,
    exerciseExampleFeature: ExerciseExampleFeature,
    private val trainingTimelineUseCase: TrainingTimelineUseCase,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
    initialState: TrainingCompletedState,
) : BaseViewModel<TrainingCompletedState, TrainingCompletedDirection, TrainingCompletedLoader>(
    initialState
), TrainingCompletedContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(TrainingScreen.Completed, reason)
    }

    init {
        safeLaunch(
            loader = TrainingCompletedLoader.SaveTraining,
            onError = { navigateTo(TrainingCompletedDirection.Back) }
        ) {
            val id = state.value.savedTrainingId ?: run {
                val duration = DateTimeUtils.ago(value = startAt)

                val domainExercises = exercises.toDomain()

                val totals = trainingTotalUseCase
                    .fromSetExercises(domainExercises)
                    .toState()

                val training = SetTraining(
                    exercises = domainExercises,
                    duration = duration,
                    volume = totals.volume.value ?: 0f,
                    intensity = totals.intensity.value ?: 0f,
                    repetitions = totals.repetitions.value ?: 0,
                )

                val saveResult = when (val trainingId = stage.id) {
                    null -> analytics.trackOperation(TrainingOperation.CreateTraining) {
                        setTrainingUseCase.execute(training).getOrThrow()
                    }
                    else -> analytics.trackOperation(TrainingOperation.UpdateTraining) {
                        updateTrainingUseCase.execute(trainingId, training).getOrThrow()
                    }
                }
                val savedId = saveResult ?: return@safeLaunch
                update { it.copy(savedTrainingId = savedId) }
                savedId
            }

            analytics.trackOperation(TrainingOperation.DeleteDraft) {
                trainingFeature.deleteDraftTraining().getOrThrow()
            }

            val domain = analytics.trackOperation(TrainingOperation.LoadTraining) {
                trainingFeature.observeTraining(id).firstOrNull()
            }

            analytics.trackOperation(TrainingOperation.RefreshExamples) {
                exerciseExampleFeature.getExerciseExamples().getOrThrow()
            }

            provideTraining(domain)
        }
    }

    private suspend fun provideTraining(value: Training?) {
        value ?: return

        val timeline = trainingTimelineUseCase
            .trainingExercises(value)
            .toState()

        val muscleLoad = muscleLoadingSummaryUseCase
            .fromTraining(value)
            .toState()

        val training = value.toState()

        update {
            it.copy(
                timeline = timeline,
                training = training,
                muscleLoad = muscleLoad,
            )
        }
    }

    override fun onSummaryClick() {
        val id = state.value.training?.id ?: return
        analytics.trackEvent(TrainingEvent.Completed.SummaryOpened)

        val dialog = DialogConfig.Statistics.Training(
            title = UiText.Res(Res.string.statistics),
            id = id
        )

        dialogController.open(dialog)
    }

    override fun onExerciseClick(id: String) {
        analytics.trackEvent(TrainingEvent.Completed.ExerciseOpened)
        val dialog = DialogConfig.Exercise(
            title = UiText.Res(Res.string.exercise_record),
            id = id,
        )

        dialogController.open(dialog)
    }

    override fun onBack() {
        analytics.trackEvent(TrainingEvent.Completed.Back)
        navigateTo(TrainingCompletedDirection.Back)
    }
}
