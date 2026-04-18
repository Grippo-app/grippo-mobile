package com.grippo.trainings.trainings

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.menu.TrainingMenu
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
import com.grippo.toolkit.date.utils.DateFormat
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
import kotlinx.datetime.LocalDateTime
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

    override fun onSelectPeriod(period: TrainingsTimelinePeriod) {
        val current = state.value
        if (current.period == period) return

        val alignedRange = period.defaultRange().coerceWithin(current.limitations)
        update { it.copy(period = period, date = alignedRange) }
    }

    override fun onOpenDateSelector() {
        safeLaunch {
            // TODO(format-state-unification): перевести TrainingsState на DateFormatState-якорь.
            // Контракт DialogConfig.DatePicker/MonthPicker — FormatState in/out, но TrainingsState.date
            // хранит сырой DateRange, поэтому здесь на входе оборачиваем `date.from` в
            // DateFormatState.of(...), а на выходе распаковываем через result.value?.let(::applyAnchor).
            // Это временный костыль до рефакторинга стейта.
            //
            // Промпт для следующей итерации:
            // 1) Добавить в TrainingsState поле `anchor: DateFormatState` (опорная дата периода),
            //    инициализировать через DateFormatState.of(anchor, limitations, DateFormat.DateOnly.DateMmmDdYyyy)
            //    везде, где сейчас формируется `date` (init-блок, onSelectPeriod, shiftDate, onOpenDaily).
            // 2) `date: DateRange` сделать производным: вычислять через `period.rangeFor(anchor.value)
            //    .coerceWithin(limitations)` в одном месте (например, в приватном `recomputeRange()`).
            // 3) Здесь заменить обёртку/развёртку на `initial = current.anchor` и
            //    `onResult = { result -> update { it.copy(anchor = result) }; result.value?.let(::applyAnchor) }`
            //    (или встроить обновление anchor прямо в applyAnchor и принимать DateFormatState).
            // 4) Проверить, что shiftDate/onOpenDaily/init-rangeFlow корректно работают с новым anchor.
            val current = state.value
            val dialog = when (current.period) {
                TrainingsTimelinePeriod.Daily -> DialogConfig.DatePicker(
                    title = stringProvider.get(Res.string.select_date),
                    initial = DateFormatState.of(
                        value = current.date.from,
                        range = current.limitations,
                        format = DateFormat.DateOnly.DateMmmDdYyyy,
                    ),
                    limitations = current.limitations,
                    onResult = { result -> result.value?.let(::applyAnchor) }
                )

                TrainingsTimelinePeriod.Monthly -> DialogConfig.MonthPicker(
                    title = stringProvider.get(Res.string.select_month),
                    initial = DateFormatState.of(
                        value = current.date.from,
                        range = current.limitations,
                        format = DateFormat.DateOnly.MmmYyyy,
                    ),
                    limitations = current.limitations,
                    onResult = { result -> result.value?.let(::applyAnchor) }
                )
            }
            dialogController.show(dialog)
        }
    }

    private fun applyAnchor(date: LocalDateTime) {
        val current = state.value
        val range = current.period.rangeFor(date).coerceWithin(current.limitations)
        update { it.copy(date = range) }
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
