package com.grippo.training.completed

import com.grippo.calculation.AnalyticsApi
import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.training.toState
import com.grippo.domain.state.training.transformation.toTrainingListValues
import com.grippo.state.domain.training.toDomain
import com.grippo.state.trainings.ExerciseState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.LocalDateTime

internal class TrainingCompletedViewModel(
    exercises: List<ExerciseState>,
    trainingFeature: TrainingFeature,
    startAt: LocalDateTime,
    private val dialogController: DialogController,
    stringProvider: StringProvider,
    colorProvider: ColorProvider,
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

            val id = trainingFeature
                .setTraining(training)
                .getOrThrow() ?: return@safeLaunch

            trainingFeature.deleteDraftTraining().getOrThrow()

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
