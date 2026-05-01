package com.grippo.trainings.trainings

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.menu.TrainingMenu
import com.grippo.core.state.trainings.TimelineState
import com.grippo.data.features.api.metrics.engagement.TrainingDigestUseCase
import com.grippo.data.features.api.training.DeleteTrainingUseCase
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
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.trainings.trainings.TrainingsDirection.Back
import com.grippo.trainings.trainings.TrainingsDirection.EditTraining
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

internal class TrainingsViewModel(
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider,
    private val trainingDigestUseCase: TrainingDigestUseCase,
    private val deleteTrainingUseCase: DeleteTrainingUseCase,
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
            .safeLaunch(loader = TrainingsLoader.Trainings)

        rangeFlow
            .flatMapLatest { range ->
                trainingFeature
                    .observeTrainings(start = range.from, end = range.to)
                    .map { trainings -> range to trainings }
            }
            .onEach { (range, trainings) ->
                provideTrainings(range, trainings)
            }
            .safeLaunch(loader = TrainingsLoader.Trainings)
    }

    private fun provideTrainings(range: DateRange, list: List<Training>) {
        val digest = trainingDigestUseCase.digest(list, range)

        val timeline = trainingTimelineUseCase.trainingTimeline(
            trainings = list,
            range = range,
            digest = digest,
        ).toState()

        update { current ->
            val nextPeriod = when (current.period) {
                is TrainingsTimelinePeriod.Daily -> TrainingsTimelinePeriod.Daily(
                    items = timeline
                        .filterIsInstance<TimelineState.Daily.Item>()
                        .toPersistentList(),
                )

                is TrainingsTimelinePeriod.Monthly -> TrainingsTimelinePeriod.Monthly.from(
                    range = range,
                    timeline = timeline,
                )
            }
            current.copy(period = nextPeriod)
        }
    }

    override fun onTrainingMenuClick(id: String) {
        val dialog = DialogConfig.TrainingMenuPicker(
            onResult = { item ->
                when (item) {
                    TrainingMenu.Delete -> deleteTraining(id)
                    TrainingMenu.Edit -> openTrainingEdit(id)
                    TrainingMenu.Details -> openTrainingOverview(id)
                }
            }
        )

        dialogController.show(dialog)
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

    override fun onSelectPeriod(id: String) {
        val current = state.value
        if (current.period.id == id) return
        val target = TrainingsTimelinePeriod.variantById(id) ?: return
        val alignedRange = target.defaultRange().coerceWithin(current.limitations)
        update { it.copy(period = target, date = alignedRange) }
    }

    override fun onOpenDateSelector() {
        safeLaunch {
            val current = state.value
            val dialog = when (current.period) {
                is TrainingsTimelinePeriod.Daily -> DialogConfig.DatePicker(
                    title = stringProvider.get(Res.string.select_date),
                    initial = current.date.from,
                    format = DateFormat.DateOnly.DateMmmDdYyyy,
                    limitations = current.limitations,
                    onResult = ::applyAnchor,
                )

                is TrainingsTimelinePeriod.Monthly -> DialogConfig.MonthPicker(
                    title = stringProvider.get(Res.string.select_month),
                    initial = current.date.from,
                    format = DateFormat.DateOnly.MmmYyyy,
                    limitations = current.limitations,
                    onResult = ::applyAnchor,
                )
            }
            dialogController.show(dialog)
        }
    }

    private fun applyAnchor(date: LocalDateTime) {
        update {
            val range = it.period.rangeFor(date).coerceWithin(it.limitations)
            it.copy(date = range)
        }
    }

    override fun onSelectNextDate() = shiftDate(1)

    override fun onSelectPreviousDate() = shiftDate(-1)

    override fun onOpenDaily(date: LocalDate) {
        val range = TrainingsTimelinePeriod.Daily()
            .rangeFor(DateTimeUtils.startOfDay(date))
            .coerceWithin(state.value.limitations)
        update {
            it.copy(
                period = TrainingsTimelinePeriod.Daily(),
                date = range,
            )
        }
    }

    private fun openTrainingOverview(id: String) {
        val config = DialogConfig.Statistics.Training(
            id = id
        )

        dialogController.show(config)
    }

    private fun deleteTraining(id: String) {
        safeLaunch {
            deleteTrainingUseCase.execute(id).getOrThrow()
        }
    }

    override fun onBack() {
        navigateTo(Back)
    }

    private fun shiftDate(direction: Int) {
        if (direction == 0) return

        val current = state.value

        val shifted = when (current.period) {
            is TrainingsTimelinePeriod.Monthly -> {
                val anchorDate = current.date.from.date.plus(DatePeriod(months = direction))
                val anchor = DateTimeUtils.startOfDay(anchorDate)
                current.period.rangeFor(anchor)
            }

            is TrainingsTimelinePeriod.Daily -> {
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
