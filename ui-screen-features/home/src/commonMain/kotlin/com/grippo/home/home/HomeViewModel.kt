package com.grippo.home.home

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.ProfileMenu
import com.grippo.core.state.profile.SettingsMenu
import com.grippo.data.features.api.metrics.ExerciseSpotlightUseCase
import com.grippo.data.features.api.metrics.MuscleLoadingUseCase
import com.grippo.data.features.api.metrics.PerformanceTrendUseCase
import com.grippo.data.features.api.metrics.TrainingStreakUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.metrics.toMonthlyDigestState
import com.grippo.domain.state.metrics.toState
import com.grippo.domain.state.metrics.toWeeklyDigestState
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
    private val muscleLoadingUseCase: MuscleLoadingUseCase,
    private val exerciseSpotlightUseCase: ExerciseSpotlightUseCase,
    private val trainingStreakUseCase: TrainingStreakUseCase,
    private val performanceTrendUseCase: PerformanceTrendUseCase,
) : BaseViewModel<HomeState, HomeDirection, HomeLoader>(
    HomeState()
), HomeContract {

    init {
        val now = DateTimeUtils.now()

        val range = DateRange(
            from = DateTimeUtils.minus(now, 90.days),
            to = now
        )

        trainingFeature
            .observeTrainings(start = range.from, end = range.to)
            .onEach(::provideTrainings)
            .safeLaunch()

        safeLaunch {
            trainingFeature.getTrainings(start = range.from, end = range.to).getOrThrow()
        }
    }

    private suspend fun provideTrainings(list: List<Training>) {
        if (list.isEmpty()) {
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
            return
        }

        val trainings = list.toState()

        val last = trainings.first()

        val weekly = trainings.toWeeklyDigestState(range = DateTimeUtils.trailingWeek())

        val monthly = trainings.toMonthlyDigestState(range = DateTimeUtils.trailingMonth())

        val totalDuration = list.fold(ZERO) { acc: Duration, item -> acc + item.duration }

        val spotlight = exerciseSpotlightUseCase.fromTrainings(list).toState()

        val streak = trainingStreakUseCase.fromTrainings(list).toState()

        val performance = performanceTrendUseCase.fromTrainings(list).toState()

        val muscleLoadSummary = muscleLoadingUseCase.fromTrainings(list).toState()

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

    override fun onOpenProfile() {
        val dialog = DialogConfig.Profile(
            onProfileResult = {
                when (it) {
                    ProfileMenu.Muscles -> navigateTo(HomeDirection.ExcludedMuscles)
                    ProfileMenu.Equipment -> navigateTo(HomeDirection.MissingEquipment)
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
}
