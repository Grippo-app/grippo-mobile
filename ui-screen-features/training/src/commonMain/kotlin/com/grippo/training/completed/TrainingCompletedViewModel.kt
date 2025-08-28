package com.grippo.training.completed

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.training.toState
import com.grippo.domain.state.training.toTrainingListValues
import com.grippo.error.provider.AppError
import com.grippo.state.domain.training.toDomain
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.trainings.ExerciseState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.LocalDateTime

internal class TrainingCompletedViewModel(
    exercises: List<ExerciseState>,
    trainingFeature: TrainingFeature,
    startAt: LocalDateTime,
    stringProvider: StringProvider,
    private val dialogController: DialogController,
) : BaseViewModel<TrainingCompletedState, TrainingCompletedDirection, TrainingCompletedLoader>(
    TrainingCompletedState()
), TrainingCompletedContract {

    init {
        safeLaunch(loader = TrainingCompletedLoader.SaveTraining) {
            val duration = DateTimeUtils.ago(startAt)

            val volume = exercises.map { exercise ->
                exercise.metrics.volume.value ?: throw AppError.Expected(
                    message = "Incorrect volume",
                    description = "value = ${exercise.metrics.volume}"
                )
            }.sum()

            val repetition = exercises.sumOf { exercise ->
                exercise.metrics.repetitions.value ?: throw AppError.Expected(
                    message = "Incorrect repetition",
                    description = "value = ${exercise.metrics.repetitions}"
                )
            }

            val intensity =
                IntensityFormatState.of(volume / repetition).value ?: throw AppError.Expected(
                    message = "Incorrect repetition",
                    description = "value = ${IntensityFormatState.of(volume / repetition).value}"
                )

            val training = SetTraining(
                exercises = exercises.toDomain(),
                duration = duration.inWholeMinutes,
                volume = volume,
                intensity = intensity,
                repetitions = repetition
            )

            val id = trainingFeature
                .setTraining(training)
                .getOrThrow() ?: return@safeLaunch

            val domain = trainingFeature.observeTraining(id).firstOrNull()
            provideTraining(domain)
        }
    }

    private fun provideTraining(value: Training?) {
        val training = value?.toState()?.toTrainingListValues() ?: return
        update { it.copy(training = training) }
    }

    override fun onExerciseClick(id: String) {
        val dialog = DialogConfig.Exercise(
            id = id,
        )

        dialogController.show(dialog)
    }

    override fun onBack() {
        navigateTo(TrainingCompletedDirection.Back)
    }
}
