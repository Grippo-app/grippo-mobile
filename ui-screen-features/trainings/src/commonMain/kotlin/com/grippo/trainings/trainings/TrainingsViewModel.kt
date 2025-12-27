package com.grippo.trainings.trainings

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.menu.MenuItemState
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.select_start_date
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.training.toState
import com.grippo.domain.state.training.transformation.transformToTrainingListValue
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.trainings.trainings.TrainingsDirection.Back
import com.grippo.trainings.trainings.TrainingsDirection.EditTraining
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

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
                val range = date.coerceWithin(limitations)
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
                val range = date.coerceWithin(limitations)
                trainingFeature.getTrainings(start = range.from, end = range.to).getOrThrow()
            }
            .safeLaunch()
    }

    private fun provideTrainings(date: DateRange, limitations: DateRange, list: List<Training>) {
        val range = date.coerceWithin(limitations)
        val trainings = list
            .toState()
            .transformToTrainingListValue(range = range)

        update { it.copy(trainings = trainings) }
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
                        TrainingMenu.Details -> openTrainingOverview(id)
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

    override fun onAddTraining() {
        navigateTo(TrainingsDirection.AddTraining)
    }

    override fun onSelectPeriod(period: TrainingsTimelinePeriod) {
        val current = state.value
        if (current.period == period) return

        val alignedRange = period.defaultRange().coerceWithin(current.limitations)
        update { it.copy(period = period, date = alignedRange) }
    }

    override fun onOpenDateSelector() {
        safeLaunch {
            val value = DateFormatState.of(
                state.value.date.from,
                state.value.limitations
            )

            val dialog = DialogConfig.DatePicker(
                title = stringProvider.get(Res.string.select_start_date),
                initial = value,
                limitations = state.value.limitations,
                onResult = { value ->
                    val date = value.value ?: return@DatePicker
                    val period = state.value.period
                    val range = period.rangeFor(date).coerceWithin(state.value.limitations)
                    update { it.copy(date = range) }
                }
            )

            dialogController.show(dialog)
        }
    }

    override fun onSelectNextDate() = shiftDate(1)

    override fun onSelectPreviousDate() = shiftDate(-1)

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

    private fun shiftDate(direction: Int) {
        if (direction == 0) return

        val current = state.value
        val span = current.date.run {
            val days = from.date.daysUntil(to.date) + 1
            if (days <= 0) 1 else days
        }
        val shiftPeriod = DatePeriod(days = span * direction)
        val shifted =
            DateTimeUtils.shift(current.date, shiftPeriod).coerceWithin(current.limitations)

        if (shifted == current.date) return

        update { it.copy(date = shifted) }
    }

    private fun DateRange.coerceWithin(limitations: DateRange): DateRange {
        val start = if (from < limitations.from) limitations.from else from
        val end = if (to > limitations.to) limitations.to else to
        return if (end < start) DateRange(start, start) else DateRange(start, end)
    }
}
