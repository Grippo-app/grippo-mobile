package com.grippo.trainings.trainings

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.menu.MenuItemState
import com.grippo.data.features.api.metrics.TrainingDigestUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.TrainingTimelineUseCase
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.select_date
import com.grippo.design.resources.provider.select_month
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.training.toState
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.trainings.trainings.TrainingsDirection.Back
import com.grippo.trainings.trainings.TrainingsDirection.EditTraining
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

internal class TrainingsViewModel(
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider,
    private val trainingDigestUseCase: TrainingDigestUseCase,
    private val trainingTimelineUseCase: TrainingTimelineUseCase,
) : BaseViewModel<TrainingsState, TrainingsDirection, TrainingsLoader>(
    TrainingsState()
), TrainingsContract {

    init {
        val rangeFlow = state
            .map { it.date to it.limitations }
            .distinctUntilChanged()
            .map { (date, limitations) -> date.coerceWithin(limitations) }

        rangeFlow
            .onEach { range ->
                trainingFeature.getTrainings(
                    start = range.from,
                    end = range.to
                ).getOrThrow()
            }
            .safeLaunch()

        rangeFlow
            .flatMapLatest { range ->
                trainingFeature
                    .observeTrainings(start = range.from, end = range.to)
                    .map { trainings -> range to trainings }
            }
            .onEach { (range, trainings) ->
                provideTrainings(range, trainings)
            }
            .safeLaunch()
    }

    private fun provideTrainings(range: DateRange, list: List<Training>) {
        val digest = trainingDigestUseCase.digest(list, range)

        val timeline = trainingTimelineUseCase.trainingTimeline(
            trainings = list,
            range = range,
            digest = digest,
        )

        update { it.copy(timeline = timeline.toState()) }
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
        val config = DialogConfig.Statistics.Trainings(
            range = state.value.date
        )

        dialogController.show(config)
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
        when (state.value.period) {
            TrainingsTimelinePeriod.Daily -> selectDate()
            TrainingsTimelinePeriod.Monthly -> selectMonth()
        }
    }

    private fun selectDate() {
        safeLaunch {
            val value = DateFormatState.of(
                state.value.date.from,
                state.value.limitations
            )

            val dialog = DialogConfig.DatePicker(
                title = stringProvider.get(Res.string.select_date),
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

    private fun selectMonth() {
        safeLaunch {
            val value = DateFormatState.of(
                state.value.date.from,
                state.value.limitations
            )

            val dialog = DialogConfig.MonthPicker(
                title = stringProvider.get(Res.string.select_month),
                initial = value,
                limitations = state.value.limitations,
                onResult = { value ->
                    val date = value.value ?: return@MonthPicker
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

    private fun openTrainingOverview(id: String) {
        val config = DialogConfig.Statistics.Training(
            id = id
        )

        dialogController.show(config)
    }

    private fun deleteTraining(id: String) {
        safeLaunch {
            trainingFeature.deleteTraining(id).getOrThrow()
        }
    }

    override fun onBack() {
        navigateTo(Back)
    }

    private fun shiftDate(direction: Int) {
        if (direction == 0) return

        val current = state.value

        val shifted = when (current.period) {
            TrainingsTimelinePeriod.Monthly -> {
                val anchorDate = current.date.from.date.plus(DatePeriod(months = direction))
                val anchor = DateTimeUtils.startOfDay(anchorDate)
                TrainingsTimelinePeriod.Monthly.rangeFor(anchor)
            }

            TrainingsTimelinePeriod.Daily -> {
                val span = current.date.run {
                    val days = from.date.daysUntil(to.date) + 1
                    if (days <= 0) 1 else days
                }
                val shiftPeriod = DatePeriod(days = span * direction)
                DateTimeUtils.shift(current.date, shiftPeriod)
            }
        }.coerceWithin(current.limitations)

        if (shifted == current.date) return

        update { it.copy(date = shifted) }
    }

    private fun DateRange.coerceWithin(limitations: DateRange): DateRange {
        val start = if (from < limitations.from) limitations.from else from
        val end = if (to > limitations.to) limitations.to else to
        return if (end < start) DateRange(start, start) else DateRange(start, end)
    }

    private fun openTrainingEdit(id: String) {
        navigateTo(EditTraining(id))
    }
}
