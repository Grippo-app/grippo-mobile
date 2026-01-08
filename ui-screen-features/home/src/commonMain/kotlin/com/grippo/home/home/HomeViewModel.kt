package com.grippo.home.home

import com.grippo.core.foundation.BaseViewModel
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
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.metrics.toState
import com.grippo.domain.state.training.toState
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.days

internal class HomeViewModel(
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
    private val exerciseSpotlightUseCase: ExerciseSpotlightUseCase,
    private val trainingStreakUseCase: TrainingStreakUseCase,
    private val performanceTrendUseCase: PerformanceTrendUseCase,
    private val trainingDigestUseCase: TrainingDigestUseCase,
    private val exerciseExampleFeature: ExerciseExampleFeature,
) : BaseViewModel<HomeState, HomeDirection, HomeLoader>(
    HomeState()
), HomeContract {

    init {
        val range = defaultRange()

        trainingFeature
            .observeTrainings(start = defaultRange().from, end = range.to)
            .onEach(::provideTrainings)
            .safeLaunch()

        safeLaunch(loader = HomeLoader.Trainings) {
            trainingFeature
                .getTrainings(start = range.from, end = range.to)
                .getOrThrow()
        }

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

        val totalDuration = list.fold(ZERO) { acc: Duration, item -> acc + item.duration }

        val weekly = trainingDigestUseCase
            .weeklyDigest(list, range = DateTimeUtils.thisWeek())
            .toState()

        val monthly = trainingDigestUseCase
            .monthlyDigest(list, range = DateTimeUtils.thisMonth())
            .toState()

        val spotlight = exerciseSpotlightUseCase
            .fromTrainings(list)
            .toState()

        val streak = trainingStreakUseCase
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

    override fun onOpenMuscleLoading() {
        val dialog = DialogConfig.MuscleLoading(
            range = defaultRange()
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

    override fun onOpenExample(id: String) {
        val dialog = DialogConfig.ExerciseExample(
            id = id
        )

        dialogController.show(dialog)
    }

    override fun onOpenTrainingStreak() {
        val dialog = DialogConfig.TrainingStreak(
            range = defaultRange()
        )

        dialogController.show(dialog)
    }

    override fun onOpenMonthlyDigest() {
        val range = DateTimeUtils.trailingMonth()
        val config = DialogConfig.Statistics.Trainings(
            range = range
        )

        dialogController.show(config)
    }

    override fun onOpenWeeklyDigest() {
        val range = DateTimeUtils.trailingWeek()
        val config = DialogConfig.Statistics.Trainings(
            range = range
        )

        dialogController.show(config)
    }

    override fun onBack() {
        navigateTo(HomeDirection.Back)
    }

    private fun defaultRange(): DateRange {
        val now = DateTimeUtils.now()

        val range = DateRange(
            from = DateTimeUtils.minus(now, 90.days),
            to = DateTimeUtils.plus(now, 1.days)
        )

        return range
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
