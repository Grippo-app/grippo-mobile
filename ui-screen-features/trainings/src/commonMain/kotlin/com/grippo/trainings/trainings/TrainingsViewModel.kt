package com.grippo.trainings.trainings

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.menu.MenuItemState
import com.grippo.core.state.profile.ProfileActivityMenu
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.date_picker_title
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
import com.grippo.trainings.trainings.utilities.pagerCombinedRange
import com.grippo.trainings.trainings.utilities.pagerRanges
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.DatePeriod

internal class TrainingsViewModel(
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider
) : BaseViewModel<TrainingsState, TrainingsDirection, TrainingsLoader>(
    TrainingsState()
), TrainingsContract {

    init {
        state
            .map { it.date }
            .distinctUntilChanged()
            .map { it.pagerCombinedRange() }
            .flatMapLatest { date ->
                trainingFeature
                    .observeTrainings(start = date.from, end = date.to)
                    .map { trainings -> date to trainings }
            }
            .onEach { (date, trainings) -> provideTrainings(date, trainings) }
            .safeLaunch()

        state
            .map { it.date }
            .distinctUntilChanged()
            .map { it.pagerCombinedRange() }
            .onEach { date ->
                trainingFeature.getTrainings(start = date.from, end = date.to).getOrThrow()
            }
            .safeLaunch()
    }

    private fun provideTrainings(date: DateRange, list: List<Training>) {
        val states = list.toState()
        val pagerRanges = date.pagerRanges()

        val trainingsByDate = pagerRanges.values
            .associateWith { range ->
                states
                    .filter { training -> training.createdAt in range }
                    .transformToTrainingListValue()
            }
            .toPersistentMap()

        update { it.copy(trainings = trainingsByDate) }
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
                    safeLaunch {
                        when (TrainingMenu.of(it)) {
                            TrainingMenu.Delete -> {
                                trainingFeature.deleteTraining(id).getOrThrow()
                            }

                            TrainingMenu.Edit -> {
                                navigateTo(EditTraining(id))
                            }

                            TrainingMenu.Overview -> {
                                val training = trainingFeature.observeTraining(id).firstOrNull()
                                    ?: return@safeLaunch

                                val config = DialogConfig.Statistics.Exercises(
                                    exercises = training.toState().exercises
                                )

                                dialogController.show(config)
                            }

                            null -> {}
                        }
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
                trainings = training.toState()
            )

            dialogController.show(config)
        }
    }

    override fun onOpenProfile() {
        val dialog = DialogConfig.Profile(
            onResult = {
                when (it) {
                    ProfileActivityMenu.ExcludedMuscles -> navigateTo(TrainingsDirection.ExcludedMuscles)
                    ProfileActivityMenu.MissingEquipment -> navigateTo(TrainingsDirection.MissingEquipment)
                    ProfileActivityMenu.Debug -> navigateTo(TrainingsDirection.Debug)
                }
            },
        )

        dialogController.show(dialog)
    }

    override fun onAddTraining() {
        navigateTo(TrainingsDirection.AddTraining)
    }

    override fun onSelectDate() {
        safeLaunch {
            val value = DateFormatState.of(state.value.date.from, state.value.limitations)

            val dialog = DialogConfig.DatePicker(
                title = stringProvider.get(Res.string.date_picker_title),
                initial = value,
                limitations = state.value.limitations,
                onResult = { value ->
                    val date = value.value ?: return@DatePicker
                    val from = DateTimeUtils.startOfDay(date)
                    val to = DateTimeUtils.endOfDay(date)
                    update { it.copy(date = DateRange(from, to)) }
                }
            )

            dialogController.show(dialog)
        }
    }

    override fun onShiftDate(days: Int) {
        if (days == 0) return

        val currentState = state.value
        val shifted = DateTimeUtils.shift(currentState.date, DatePeriod(days = days))

        val limitations = currentState.limitations
        val exceedsLimitations = shifted.from < limitations.from || shifted.to > limitations.to
        if (exceedsLimitations) return

        update { it.copy(date = shifted) }
    }

    override fun onBack() {
        navigateTo(Back)
    }
}
