package com.grippo.home.home

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.metrics.PerformanceMetricTypeState
import com.grippo.core.state.profile.ProfileMenu
import com.grippo.core.state.profile.SettingsMenu
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.metrics.ExerciseSpotlightUseCase
import com.grippo.data.features.api.metrics.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.PerformanceTrendUseCase
import com.grippo.data.features.api.metrics.TrainingDigestUseCase
import com.grippo.data.features.api.metrics.TrainingStreakUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.period_picker_title
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.metrics.toState
import com.grippo.domain.state.training.toState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

internal class HomeViewModel(
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
    private val exerciseSpotlightUseCase: ExerciseSpotlightUseCase,
    private val trainingStreakUseCase: TrainingStreakUseCase,
    private val performanceTrendUseCase: PerformanceTrendUseCase,
    private val trainingDigestUseCase: TrainingDigestUseCase,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val stringProvider: StringProvider,
) : BaseViewModel<HomeState, HomeDirection, HomeLoader>(
    HomeState()
), HomeContract {

    init {
        state
            .map { it.period.value }
            .filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { period ->
                trainingFeature
                    .observeTrainings(start = period.from, end = period.to)
                    .onEach(::provideTrainings)
            }.safeLaunch()

        state
            .map { it.period.value }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { period ->
                trainingFeature
                    .getTrainings(start = period.from, end = period.to)
                    .getOrThrow()
            }.safeLaunch()

        safeLaunch {
            exerciseExampleFeature.getExerciseExamples()
        }
    }

    private suspend fun provideTrainings(list: List<Training>) {
        if (list.isEmpty()) {
            clearHome()
            return
        }

        val trainings = list.toState()

        val last = trainings.firstOrNull() ?: return

        val totalDuration = list.fold(ZERO) { acc: Duration, item ->
            acc + item.duration
        }

        val streak = trainingStreakUseCase
            .fromTrainings(list)
            .toState()

        val weekly = trainingDigestUseCase
            .weeklyDigest(list, range = DateRange.Range.Weekly().range)
            .toState()

        val monthly = trainingDigestUseCase
            .monthlyDigest(list, range = DateRange.Range.Monthly().range)
            .toState()

        val spotlight = exerciseSpotlightUseCase
            .fromTrainings(list)
            .toState()

        val performance = performanceTrendUseCase
            .fromTrainings(list)
            .toState()

        val muscleLoadSummary = muscleLoadingSummaryUseCase
            .fromTrainings(list)
            .toState()

        update {
            it.copy(
                weeklyDigest = weekly,
                monthlyDigest = monthly,
                totalDuration = totalDuration,
                spotlight = spotlight,
                muscleLoad = muscleLoadSummary,
                streak = streak,
                performance = performance,
                lastTraining = last
            )
        }
    }

    override fun onStartTraining() {
        navigateTo(HomeDirection.AddTraining)
    }

    override fun onPerformanceMetricClick(type: PerformanceMetricTypeState) {
        val range = state.value.period.value ?: return

        val dialog = DialogConfig.PerformanceTrend(
            range = range,
            metricType = type,
        )

        dialogController.show(dialog)
    }

    override fun onOpenMuscleLoading() {
        val range = state.value.period.value ?: return

        val dialog = DialogConfig.MuscleLoading(
            range = range,
        )
        dialogController.show(dialog)
    }

    override fun onOpenProfile() {
        val dialog = DialogConfig.Profile(
            onProfileResult = {
                when (it) {
                    ProfileMenu.Muscles -> navigateTo(HomeDirection.ExcludedMuscles)
                    ProfileMenu.Equipment -> navigateTo(HomeDirection.MissingEquipment)
                    ProfileMenu.Experience -> navigateTo(HomeDirection.Experience)
                    ProfileMenu.Settings -> navigateTo(HomeDirection.Settings)
                }
            },
            onSettingsResult = {
                when (it) {
                    SettingsMenu.Debug -> navigateTo(HomeDirection.Debug)
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onOpenTrainings() {
        navigateTo(HomeDirection.Trainings)
    }

    override fun onOpenPeriodPicker() {
        safeLaunch {
            val dialog = DialogConfig.PeriodPicker(
                title = stringProvider.get(Res.string.period_picker_title),
                initial = state.value.period,
                onResult = { result -> update { s -> s.copy(period = result) } }
            )

            dialogController.show(dialog)
        }
    }

    override fun onOpenExample(id: String) {
        val dialog = DialogConfig.ExerciseExample(
            id = id
        )

        dialogController.show(dialog)
    }

    override fun onOpenTrainingStreak() {
        val range = state.value.period.value ?: return

        val dialog = DialogConfig.TrainingStreak(
            range = range
        )

        dialogController.show(dialog)
    }

    override fun onOpenMonthlyDigest() {
        val range = DateRange.Range.Last30Days().range
        val config = DialogConfig.Statistics.Trainings(
            range = range
        )

        dialogController.show(config)
    }

    override fun onOpenWeeklyDigest() {
        val range = DateRange.Range.Last7Days().range
        val config = DialogConfig.Statistics.Trainings(
            range = range
        )

        dialogController.show(config)
    }

    override fun onBack() {
        navigateTo(HomeDirection.Back)
    }

    private fun clearHome() {
        update {
            it.copy(
                weeklyDigest = null,
                monthlyDigest = null,
                totalDuration = null,
                spotlight = null,
                muscleLoad = null,
                streak = null,
                performance = emptyList(),
                lastTraining = null
            )
        }
    }
}
