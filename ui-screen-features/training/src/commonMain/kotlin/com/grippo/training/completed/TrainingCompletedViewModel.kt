package com.grippo.training.completed

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.metrics.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.TrainingTotalUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.TrainingTimelineUseCase
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.metrics.toState
import com.grippo.domain.state.training.toState
import com.grippo.services.firebase.FirebaseProvider
import com.grippo.services.firebase.FirebaseProvider.Event
import com.grippo.state.domain.training.toDomain
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.LocalDateTime

internal class TrainingCompletedViewModel(
    stage: StageState,
    exercises: List<ExerciseState>,
    trainingFeature: TrainingFeature,
    startAt: LocalDateTime,
    private val trainingTotalUseCase: TrainingTotalUseCase,
    private val dialogController: DialogController,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val trainingTimelineUseCase: TrainingTimelineUseCase,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
) : BaseViewModel<TrainingCompletedState, TrainingCompletedDirection, TrainingCompletedLoader>(
    TrainingCompletedState()
), TrainingCompletedContract {

    init {
        FirebaseProvider.logEvent(Event.WORKOUT_COMPLETED)

        safeLaunch(loader = TrainingCompletedLoader.SaveTraining) {
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
                repetitions = totals.repetitions.value ?: 0
            )

            val id = when (val allocatedId = stage.id) {
                null -> {
                    val result = trainingFeature
                        .setTraining(training)
                        .getOrThrow() ?: return@safeLaunch
                    result
                }

                else -> {
                    val result = trainingFeature
                        .updateTraining(allocatedId, training)
                        .getOrThrow() ?: return@safeLaunch
                    result
                }
            }

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

        val summary = muscleLoadingSummaryUseCase
            .fromTraining(value)
            .toState()

        val training = value
            .toState()

        update {
            it.copy(
                timeline = timeline,
                training = training,
                summary = summary,
            )
        }
    }

    override fun onSummaryClick() {
        val id = state.value.training?.id ?: return

        val dialog = DialogConfig.Statistics.Training(
            id = id
        )

        dialogController.show(dialog)
    }

    override fun onExerciseClick(id: String) {
        val dialog = DialogConfig.Exercise(id = id)

        dialogController.show(dialog)
    }

    override fun onBack() {
        navigateTo(TrainingCompletedDirection.Back)
    }
}
