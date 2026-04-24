package com.grippo.training.completed

import com.grippo.core.error.provider.AppError
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.metrics.distribution.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.performance.PerformanceTrendUseCase
import com.grippo.data.features.api.metrics.volume.TrainingTotalUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.TrainingTimelineUseCase
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.error_save_training_description
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.something_went_wrong
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.metrics.distribution.toState
import com.grippo.domain.state.metrics.performance.toState
import com.grippo.domain.state.metrics.volume.toState
import com.grippo.domain.state.training.toState
import com.grippo.services.firebase.FirebaseProvider
import com.grippo.services.firebase.FirebaseProvider.Event
import com.grippo.state.domain.training.toDomain
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.LocalDateTime

internal class TrainingCompletedViewModel(
    stage: StageState,
    exercises: List<ExerciseState>,
    startAt: LocalDateTime,
    private val trainingFeature: TrainingFeature,
    private val trainingTotalUseCase: TrainingTotalUseCase,
    private val dialogController: DialogController,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val trainingTimelineUseCase: TrainingTimelineUseCase,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
    private val performanceTrendUseCase: PerformanceTrendUseCase,
    private val stringProvider: StringProvider,
) : BaseViewModel<TrainingCompletedState, TrainingCompletedDirection, TrainingCompletedLoader>(
    TrainingCompletedState()
), TrainingCompletedContract {

    private companion object {
        private const val MIN_TRAININGS_FOR_TREND = 3
    }

    init {
        FirebaseProvider.logEvent(Event.WORKOUT_COMPLETED)

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
                repetitions = totals.repetitions.value ?: 0
            )

            val id = when (val allocatedId = stage.id) {
                null -> {
                    val result = trainingFeature
                        .setTraining(training)
                        .onFailure {
                            throw AppError.Expected(
                                message = stringProvider.get(Res.string.something_went_wrong),
                                description = stringProvider.get(Res.string.error_save_training_description)
                            )
                        }.getOrThrow() ?: return@safeLaunch

                    result
                }

                else -> {
                    val result = trainingFeature
                        .updateTraining(allocatedId, training)
                        .onFailure {
                            throw AppError.Expected(
                                message = stringProvider.get(Res.string.something_went_wrong),
                                description = stringProvider.get(Res.string.error_save_training_description)
                            )
                        }.getOrThrow() ?: return@safeLaunch

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

        val muscleLoad = muscleLoadingSummaryUseCase
            .fromTraining(value)
            .toState()

        val training = value.toState()
        val volumeTrend = resolveVolumeTrend(latest = value)

        update {
            it.copy(
                timeline = timeline,
                training = training,
                muscleLoad = muscleLoad,
                volumeTrend = volumeTrend,
            )
        }
    }

    private suspend fun resolveVolumeTrend(
        latest: Training,
    ): PerformanceMetricState.Volume? {
        val infinity = DateRangePresets.infinity()

        trainingFeature
            .getTrainings(start = infinity.from, end = infinity.to)
            .getOrElse { return null }

        val stored = trainingFeature
            .observeTrainings(start = infinity.from, end = infinity.to)
            .firstOrNull()
            .orEmpty()

        // Guarantee the freshly saved training is present exactly once. The
        // trainings flow may not have re-emitted after the write, or may still
        // hold a pre-update copy with the same id — re-inserting `latest`
        // covers both cases. PerformanceTrendUseCase sorts by createdAt
        // internally, so list order here is irrelevant.
        val timeline = stored.filterNot { it.id == latest.id } + latest
        if (timeline.size < MIN_TRAININGS_FOR_TREND) return null

        return performanceTrendUseCase
            .fromTrainings(timeline)
            .toState()
            .filterIsInstance<PerformanceMetricState.Volume>()
            .firstOrNull()
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
