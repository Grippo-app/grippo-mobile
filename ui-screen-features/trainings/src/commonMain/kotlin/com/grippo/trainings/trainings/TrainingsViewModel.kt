package com.grippo.trainings.trainings

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.menu.MenuItemState
import com.grippo.core.state.profile.ProfileMenu
import com.grippo.core.state.profile.SettingsMenu
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.training.toState
import com.grippo.domain.state.training.transformation.transformToTrainingListValue
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.date.utils.contains
import com.grippo.trainings.trainings.TrainingsDirection.Back
import com.grippo.trainings.trainings.TrainingsDirection.EditTraining
import com.grippo.trainings.trainings.utilities.coerceWithin
import com.grippo.trainings.trainings.utilities.pagerCombinedRange
import com.grippo.trainings.trainings.utilities.pagerRanges
import com.grippo.trainings.trainings.utilities.shiftForPager
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDate

internal class TrainingsViewModel(
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider
) : BaseViewModel<TrainingsState, TrainingsDirection, TrainingsLoader>(
    TrainingsState()
), TrainingsContract {

    init {
        state
            .map { it.date to it.limitations }
            .distinctUntilChanged()
            .flatMapLatest { (date, limitations) ->
                val range = date.pagerCombinedRange(limitations)
                trainingFeature
                    .observeTrainings(start = range.from, end = range.to)
                    .map { trainings -> Triple(date, limitations, trainings) }
            }
            .onEach { (date, limitations, trainings) ->
                provideTrainings(date, limitations, trainings)
            }
            .safeLaunch()

        state
            .map { it.date to it.limitations }
            .distinctUntilChanged()
            .onEach { (date, limitations) ->
                val range = date.pagerCombinedRange(limitations)
                trainingFeature.getTrainings(start = range.from, end = range.to).getOrThrow()
            }
            .safeLaunch()
    }

    private fun provideTrainings(date: DateRange, limitations: DateRange, list: List<Training>) {
        val states = list.toState()
        val pagerRanges = date.pagerRanges(limitations)

        val trainingsByOffset = pagerRanges.mapValues { (_, range) ->
            states
                .filter { training -> training.createdAt in range }
                .transformToTrainingListValue(range = range)
        }.toPersistentMap()

        update { it.copy(trainings = trainingsByOffset) }
    }

    override fun onTrainingMenuClick(id: String) {
        safeLaunch {
            val list = TrainingMenu.entries.map {
                MenuItemState(
                    id = it.id,
                    text = it.text(stringProvider)
                )
            }

            val dialog = DialogConfig.MenuPicker(
                items = list,
                onResult = {
                    when (TrainingMenu.of(it)) {
                        TrainingMenu.Delete -> deleteTraining(id)
                        TrainingMenu.Edit -> openTrainingEdit(id)
                        TrainingMenu.Overview -> openTrainingOverview(id)
                        null -> {}
                    }
                }
            )

            dialogController.show(dialog)
        }
    }

    override fun onExerciseClick(id: String) {
        val dialog = DialogConfig.Exercise(
            id = id,
        )

        dialogController.show(dialog)
    }

    override fun onDailyDigestViewStats() {
        safeLaunch {
            val training = trainingFeature
                .observeTrainings(state.value.date.from, state.value.date.to)
                .firstOrNull()
                ?: return@safeLaunch

            val config = DialogConfig.Statistics.Trainings(
                trainings = training.toState(),
                range = state.value.date
            )

            dialogController.show(config)
        }
    }

    override fun onOpenProfile() {
        val dialog = DialogConfig.Profile(
            onProfileResult = {
                when (it) {
                    ProfileMenu.Muscles -> navigateTo(TrainingsDirection.ExcludedMuscles)
                    ProfileMenu.Equipment -> navigateTo(TrainingsDirection.MissingEquipment)
                }
            },
            onSettingsResult = {
                when (it) {
                    SettingsMenu.Debug -> navigateTo(TrainingsDirection.Debug)
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onAddTraining() {
        navigateTo(TrainingsDirection.AddTraining)
    }

    override fun onShiftDate(days: Int) {
        if (days == 0) return

        val currentState = state.value
        val limitations = currentState.limitations
        val shifted = currentState.date.shiftForPager(days).coerceWithin(limitations)

        if (shifted == currentState.date) return

        update { it.copy(date = shifted) }
    }

    override fun onSelectPeriod(period: TrainingsTimelinePeriod) {
        val current = state.value
        if (current.period == period) return

        val alignedRange = period.defaultRange().coerceWithin(current.limitations)
        update { it.copy(period = period, date = alignedRange) }
    }

    override fun onOpenDaily(date: LocalDate) {
        val range = TrainingsTimelinePeriod.Daily.rangeFor(DateTimeUtils.startOfDay(date))
        val aligned = range.coerceWithin(state.value.limitations)
        update { it.copy(period = TrainingsTimelinePeriod.Daily, date = aligned) }
    }

    private fun openTrainingEdit(id: String) {
        navigateTo(EditTraining(id))
    }

    private fun openTrainingOverview(id: String) = safeLaunch {
        val training = trainingFeature.observeTraining(id).firstOrNull()
            ?: return@safeLaunch

        val config = DialogConfig.Statistics.Exercises(
            exercises = training.toState().exercises
        )

        dialogController.show(config)
    }

    private fun deleteTraining(id: String) = safeLaunch {
        trainingFeature.deleteTraining(id).getOrThrow()
    }

    override fun onBack() {
        navigateTo(Back)
    }
}
