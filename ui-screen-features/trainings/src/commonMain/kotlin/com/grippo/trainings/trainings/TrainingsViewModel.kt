package com.grippo.trainings.trainings

import com.grippo.analytics.trainings.TrainingsAnalytics
import com.grippo.analytics.trainings.models.TrainingsEvent
import com.grippo.analytics.trainings.models.TrainingsOperation
import com.grippo.analytics.trainings.models.TrainingsScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.menu.TrainingMenu
import com.grippo.core.state.trainings.TimelineState
import com.grippo.data.features.api.metrics.engagement.TrainingDigestUseCase
import com.grippo.data.features.api.training.DeleteTrainingUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.TrainingTimelineUseCase
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_record
import com.grippo.design.resources.provider.menu
import com.grippo.design.resources.provider.select_date
import com.grippo.design.resources.provider.select_month
import com.grippo.design.resources.provider.statistics
import com.grippo.design.resources.provider.value_statistics
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.training.toState
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.trainings.trainings.TrainingsDirection.Back
import com.grippo.trainings.trainings.TrainingsDirection.EditTraining
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

internal class TrainingsViewModel(
    private val analytics: TrainingsAnalytics,
    private val trainingFeature: TrainingFeature,
    private val dialogController: DialogController,
    private val trainingDigestUseCase: TrainingDigestUseCase,
    private val deleteTrainingUseCase: DeleteTrainingUseCase,
    private val trainingTimelineUseCase: TrainingTimelineUseCase,
    initialState: TrainingsState?,
) : BaseViewModel<TrainingsState, TrainingsDirection, TrainingsLoader>(
    (initialState ?: TrainingsState(date = TrainingsTimelinePeriod.Monthly().defaultRange())).let { draft ->
        draft.selectRange(draft.date)
    }
), TrainingsContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(TrainingsScreen.Calendar, reason)
    }

    init {
        val rangeFlow = state
            .map { it.period.effectiveRange(it.date.from, it.limitations) }
            .distinctUntilChanged()

        rangeFlow
            .mapLatest { range ->
                withLoader(TrainingsLoader.Trainings) {
                    analytics.trackOperation(TrainingsOperation.RefreshTrainings) {
                        trainingFeature.getTrainings(
                            start = range.from,
                            end = range.to
                        ).getOrThrow()
                    }
                }
            }
            .safeLaunch()

        rangeFlow
            .flatMapLatest { range ->
                trainingFeature
                    .observeTrainings(start = range.from, end = range.to)
                    .map { trainings -> range to trainings }
            }
            .onEach { (range, trainings) -> provideTrainings(range, trainings) }
            .safeLaunch()
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
        analytics.trackEvent(TrainingsEvent.TrainingMenuClicked)
        val dialog = DialogConfig.TrainingMenuPicker(
            title = UiText.Res(Res.string.menu),
            onResult = { item ->
                when (item) {
                    TrainingMenu.Delete -> {
                        analytics.trackEvent(TrainingsEvent.DeleteSelected)
                        deleteTraining(id)
                    }
                    TrainingMenu.Edit -> {
                        analytics.trackEvent(TrainingsEvent.EditSelected)
                        openTrainingEdit(id)
                    }
                    TrainingMenu.Details -> {
                        analytics.trackEvent(TrainingsEvent.DetailsSelected)
                        openTrainingOverview(id)
                    }
                }
            }
        )

        dialogController.open(dialog)
    }

    override fun onExerciseClick(id: String) {
        analytics.trackEvent(TrainingsEvent.ExerciseClicked)
        val dialog = DialogConfig.Exercise(
            title = UiText.Res(Res.string.exercise_record),
            id = id,
        )

        dialogController.open(dialog)
    }

    override fun onDailyDigestViewStats() {
        analytics.trackEvent(TrainingsEvent.DigestClicked)
        val range = state.value.date
        val rangeLabel = DateRangeFormatState.of(range).label()
        val title = rangeLabel?.let {
            UiText.Res(Res.string.value_statistics, persistentListOf(it))
        } ?: UiText.Res(Res.string.statistics)
        val config = DialogConfig.Statistics.Trainings(
            title = title,
            range = range
        )

        dialogController.open(config)
    }

    override fun onSelectPeriod(id: String) {
        val current = state.value
        if (current.period.id == id) return
        val target = TrainingsTimelinePeriod.variantById(id) ?: return
        analytics.trackEvent(
            when (target) {
                is TrainingsTimelinePeriod.Daily -> TrainingsEvent.DailyPeriodSelected
                is TrainingsTimelinePeriod.Monthly -> TrainingsEvent.MonthlyPeriodSelected
            }
        )
        val alignedRange = target.defaultRange().coerceWithin(current.limitations)
        update { it.copy(period = target).selectRange(alignedRange) }
    }

    override fun onOpenDateSelector() {
        val current = state.value
        analytics.trackEvent(
            when (current.period) {
                is TrainingsTimelinePeriod.Daily -> TrainingsEvent.DatePickerClicked
                is TrainingsTimelinePeriod.Monthly -> TrainingsEvent.MonthPickerClicked
            }
        )
        val dialog = when (current.period) {
            is TrainingsTimelinePeriod.Daily -> DialogConfig.DatePicker(
                title = UiText.Res(Res.string.select_date),
                initial = current.date.from,
                format = DateFormat.DateOnly.DateMmmDdYyyy,
                limitations = current.limitations,
                onResult = { date ->
                    analytics.trackEvent(TrainingsEvent.DateConfirmed)
                    applyAnchor(date)
                },
            )

            is TrainingsTimelinePeriod.Monthly -> DialogConfig.MonthPicker(
                title = UiText.Res(Res.string.select_month),
                initial = current.date.from,
                format = DateFormat.DateOnly.MmmYyyy,
                limitations = current.limitations,
                onResult = { date ->
                    analytics.trackEvent(TrainingsEvent.MonthConfirmed)
                    applyAnchor(date)
                },
            )
        }
        dialogController.open(dialog)
    }

    private fun applyAnchor(date: LocalDateTime) {
        update {
            val range = it.period.rangeFor(date).coerceWithin(it.limitations)
            if (it.date == range) it else it.selectRange(range)
        }
    }

    override fun onSelectNextDate() = shiftDate(1)

    override fun onSelectPreviousDate() = shiftDate(-1)

    override fun onOpenDaily(date: LocalDate) {
        analytics.trackEvent(TrainingsEvent.DailySelected)
        val range = TrainingsTimelinePeriod.Daily()
            .rangeFor(DateTimeUtils.startOfDay(date))
            .coerceWithin(state.value.limitations)
        update { it.copy(period = TrainingsTimelinePeriod.Daily(), date = range) }
    }

    private fun openTrainingOverview(id: String) {
        val config = DialogConfig.Statistics.Training(
            title = UiText.Res(Res.string.statistics),
            id = id
        )

        dialogController.open(config)
    }

    private fun deleteTraining(id: String) {
        safeLaunch {
            analytics.trackOperation(TrainingsOperation.DeleteTraining) {
                deleteTrainingUseCase.execute(id).getOrThrow()
            }
        }
    }

    override fun onBack() {
        analytics.trackEvent(TrainingsEvent.Back)
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

        analytics.trackEvent(
            if (direction > 0) TrainingsEvent.NextDateSelected else TrainingsEvent.PreviousDateSelected
        )
        update { it.selectRange(shifted) }
    }

    private fun openTrainingEdit(id: String) {
        navigateTo(EditTraining(id))
    }
}
