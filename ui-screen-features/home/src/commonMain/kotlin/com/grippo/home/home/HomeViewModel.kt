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
import com.grippo.data.features.api.metrics.TrainingLoadProfileUseCase
import com.grippo.data.features.api.metrics.TrainingStreakUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetDraftTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.period_picker_title
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.metrics.toState
import com.grippo.domain.state.training.toState
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
    private val trainingLoadProfileUseCase: TrainingLoadProfileUseCase,
    private val stringProvider: StringProvider,
) : BaseViewModel<HomeState, HomeDirection, HomeLoader>(
    HomeState()
), HomeContract {

    init {
        state
            .map { it.range.range }
            .filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { period ->
                trainingFeature
                    .observeTrainings(start = period.from, end = period.to)
                    .onEach(::provideTrainings)
            }.safeLaunch()

        state
            .map { it.range.range }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { period ->
                trainingFeature
                    .getTrainings(start = period.from, end = period.to)
                    .getOrThrow()
            }
            .safeLaunch(loader = HomeLoader.Trainings)

        safeLaunch {
            exerciseExampleFeature.getExerciseExamples()
        }

        trainingFeature.getDraftTraining()
            .onEach(::provideDraftTraining)
            .safeLaunch()
    }

    private fun provideDraftTraining(value: SetDraftTraining?) {
        val hasDraftTraining = value != null
        update { it.copy(hasDraftTraining = hasDraftTraining) }
    }

    private suspend fun provideTrainings(list: List<Training>) {
        val range = state.value.range.range ?: return

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

        val digest = trainingDigestUseCase
            .digest(list, range = range)
            .toState()

        val consistent = exerciseSpotlightUseCase
            .mostConsistent(list)
            ?.toState()

        val best = exerciseSpotlightUseCase
            .bestProgress(list)
            ?.toState()

        val missing = exerciseSpotlightUseCase
            .comebackMissing(list)
            ?.toState()

        val performance = performanceTrendUseCase
            .fromTrainings(list)
            .toState()

        val muscleLoadSummary = muscleLoadingSummaryUseCase
            .fromTrainings(list)
            .toState()

        val profile = trainingLoadProfileUseCase
            .fromTrainings(list)
            .toState()

        update {
            it.copy(
                digest = digest,
                totalDuration = totalDuration,
                missing = missing,
                best = best,
                consistent = consistent,
                muscleLoad = muscleLoadSummary,
                streak = streak,
                performance = performance,
                lastTraining = last,
                profile = profile
            )
        }
    }

    override fun onStartTraining() {
        navigateTo(HomeDirection.AddTraining)
    }

    override fun onPerformanceMetricClick(type: PerformanceMetricTypeState) {
        val range = state.value.range.range ?: return

        val dialog = DialogConfig.PerformanceTrend(
            range = range,
            metricType = type,
        )

        dialogController.show(dialog)
    }

    override fun onOpenMuscleLoading() {
        val range = state.value.range.range ?: return

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
                    ProfileMenu.Body -> navigateTo(HomeDirection.Body)
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
                initial = state.value.range,
                onResult = { result -> update { s -> s.copy(range = result) } }
            )

            dialogController.show(dialog)
        }
    }

    override fun onOpenTrainingProfile() {
        val range = state.value.range.range ?: return

        val dialog = DialogConfig.TrainingProfile(
            range = range
        )

        dialogController.show(dialog)
    }

    override fun onOpenExample(id: String) {
        val dialog = DialogConfig.ExerciseExample(
            id = id
        )

        dialogController.show(dialog)
    }

    override fun onResumeTraining() {
        val config = DialogConfig.DraftTraining(
            onContinue = { navigateTo(HomeDirection.DraftTraining) },
            onStartNew = { navigateTo(HomeDirection.AddTraining) }
        )

        dialogController.show(config)
    }

    override fun onOpenTrainingStreak() {
        val range = state.value.range.range ?: return

        val dialog = DialogConfig.TrainingStreak(
            range = range
        )

        dialogController.show(dialog)
    }

    override fun onOpenDigest() {
        val range = state.value.range.range ?: return

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
                digest = null,
                totalDuration = null,
                missing = null,
                best = null,
                consistent = null,
                muscleLoad = null,
                streak = null,
                performance = emptyList(),
                lastTraining = null,
                profile = null
            )
        }
    }
}
