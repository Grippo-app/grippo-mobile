package com.grippo.training.completed

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.training.toState
import com.grippo.domain.state.training.transformation.toTrainingListValues
import com.grippo.state.domain.training.toDomain
import com.grippo.toolkit.calculation.AnalyticsApi
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.LocalDateTime

internal class TrainingCompletedViewModel(
    stage: StageState,
    exercises: List<ExerciseState>,
    trainingFeature: TrainingFeature,
    startAt: LocalDateTime,
    stringProvider: StringProvider,
    colorProvider: ColorProvider,
    private val dialogController: DialogController,
    private val exerciseExampleFeature: ExerciseExampleFeature
) : BaseViewModel<TrainingCompletedState, TrainingCompletedDirection, TrainingCompletedLoader>(
    TrainingCompletedState()
), TrainingCompletedContract {

    private val analytics = AnalyticsApi(stringProvider, colorProvider)

    init {
        safeLaunch(loader = TrainingCompletedLoader.SaveTraining) {
            val duration = DateTimeUtils.ago(
                value = startAt
            )

            val totals = analytics.metricsFromExercises(
                exercises = exercises
            )

            val training = SetTraining(
                exercises = exercises.toDomain(),
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
