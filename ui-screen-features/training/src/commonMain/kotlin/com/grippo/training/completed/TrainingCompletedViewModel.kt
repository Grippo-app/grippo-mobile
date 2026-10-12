package com.grippo.training.completed

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
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.LocalDateTime

internal class TrainingCompletedViewModel(
    stage: StageState,
    exercises: List<ExerciseState>,
    startAt: LocalDateTime,
    private val trainingFeature: TrainingFeature,
    setTrainingUseCase: SetTrainingUseCase,
    updateTrainingUseCase: UpdateTrainingUseCase,
    trainingTotalUseCase: TrainingTotalUseCase,
    private val dialogController: DialogController,
    exerciseExampleFeature: ExerciseExampleFeature,
    private val trainingTimelineUseCase: TrainingTimelineUseCase,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
) : BaseViewModel<TrainingCompletedState, TrainingCompletedDirection, TrainingCompletedLoader>(
    TrainingCompletedState()
), TrainingCompletedContract {

    init {
        safeLaunch(
            loader = TrainingCompletedLoader.SaveTraining,
            onError = ::onBack
        ) {
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
                null -> setTrainingUseCase.execute(training)
                else -> updateTrainingUseCase.execute(trainingId, training)
            }
            val id = saveResult.getOrThrow() ?: return@safeLaunch

            trainingFeature.deleteDraftTraining().getOrThrow()

            val domain = trainingFeature.observeTraining(id).firstOrNull()

            exerciseExampleFeature.getExerciseExamples().getOrThrow()

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

        val dialog = DialogConfig.Statistics.Training(
            title = UiText.Res(Res.string.statistics),
            id = id
        )

        dialogController.open(dialog)
    }

    override fun onExerciseClick(id: String) {
        val dialog = DialogConfig.Exercise(
            title = UiText.Res(Res.string.exercise_record),
            id = id,
        )

        dialogController.open(dialog)
    }

    override fun onBack() {
        navigateTo(TrainingCompletedDirection.Back)
    }
}
