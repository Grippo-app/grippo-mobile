package com.grippo.home.home

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.profile.ProfileMenu
import com.grippo.core.state.profile.SettingsMenu
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.training.toState
import com.grippo.domain.state.training.transformation.toHighlight
import com.grippo.domain.state.training.transformation.toMonthlyDigestState
import com.grippo.domain.state.training.transformation.toWeeklyDigestState
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.days

internal class HomeViewModel(
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val exerciseExampleFeature: ExerciseExampleFeature,
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
                    weeklyDigestState = null,
                    monthlyDigestState = null,
                    highlight = null,
                    lastTraining = null
                )
            }
            return
        }

        val trainings = list.toState()

        val last = trainings.first()

        val weekly = trainings.toWeeklyDigestState(range = DateTimeUtils.trailingWeek())

        val monthly = trainings.toMonthlyDigestState(range = DateTimeUtils.trailingMonth())

        val exampleIds = list
            .flatMap { training -> training.exercises }
            .map { exercise -> exercise.exerciseExample.id }
            .toSet()

        val examples: List<ExerciseExampleState> = if (exampleIds.isEmpty()) {
            emptyList()
        } else {
            exerciseExampleFeature
                .observeExerciseExamples(exampleIds.toList())
                .first()
                .toState()
        }

        val highlight = trainings.toHighlight(exerciseExamples = examples)

        update {
            it.copy(
                weeklyDigestState = weekly,
                monthlyDigestState = monthly,
                highlight = highlight,
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
