package com.grippo.home.home

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.ProfileMenu
import com.grippo.core.state.profile.SettingsMenu
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.training.toState
import com.grippo.domain.state.training.transformation.toMonthlyDigestState
import com.grippo.domain.state.training.transformation.toWeeklyDigestState
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.onEach

internal class HomeViewModel(
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
) : BaseViewModel<HomeState, HomeDirection, HomeLoader>(
    HomeState()
), HomeContract {

    private val weeklyRange: DateRange = DateTimeUtils.thisWeek()
    private val monthlyRange: DateRange = DateTimeUtils.thisMonth()

    init {
        trainingFeature
            .observeLastTraining()
            .onEach(::provideLastTraining)
            .safeLaunch()

        trainingFeature
            .observeTrainings(start = weeklyRange.from, end = weeklyRange.to)
            .onEach(::provideWeeklyDigest)
            .safeLaunch()

        trainingFeature
            .observeTrainings(start = monthlyRange.from, end = monthlyRange.to)
            .onEach(::provideMonthlyDigest)
            .safeLaunch()

        safeLaunch {
            trainingFeature.getTrainings(
                start = weeklyRange.from,
                end = weeklyRange.to
            ).getOrThrow()
        }

        safeLaunch {
            trainingFeature.getTrainings(
                start = monthlyRange.from,
                end = monthlyRange.to
            ).getOrThrow()
        }
    }

    private fun provideLastTraining(value: Training?) {
        val training = value?.toState() ?: return
        update { it.copy(lastTraining = training) }
    }

    private fun provideWeeklyDigest(trainings: List<Training>) {
        if (trainings.isEmpty()) {
            update { it.copy(weeklyDigestState = null) }
            return
        }

        val digest = trainings
            .toState()
            .toWeeklyDigestState(range = weeklyRange)

        update { it.copy(weeklyDigestState = digest) }
    }

    private fun provideMonthlyDigest(trainings: List<Training>) {
        if (trainings.isEmpty()) {
            update { it.copy(monthlyDigestState = null) }
            return
        }

        val digest = trainings
            .toState()
            .toMonthlyDigestState(range = monthlyRange)

        update { it.copy(monthlyDigestState = digest) }
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

    override fun onBack() {
        navigateTo(HomeDirection.Back)
    }
}
