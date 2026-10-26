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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
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
    state = (initialState ?: TrainingsState(
        date = TrainingsTimelinePeriod.Monthly().defaultRange(),
    )).let { draft ->
        draft.selectRange(draft.date)
    }
), TrainingsContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(TrainingsScreen.Calendar, reason)
    }

    init {
        val rangeFlow = state
            .map { current ->
                current.period.effectiveRange(
                    anchor = current.date.from,
                    limitations = current.limitations,
                )
            }
            .distinctUntilChanged()

        rangeFlow
            .mapLatest(::refreshTrainings)
            .safeLaunch()

        rangeFlow
            .flatMapLatest { range ->
                trainingFeature.observeTrainings(start = range.from, end = range.to)
                    .map { trainings -> range to trainings }
            }
            .onEach(::provideTrainings)
            .safeLaunch()
    }

    private suspend fun refreshTrainings(range: DateRange) {
        withLoader(loader = TrainingsLoader.Trainings) {
            val refresh = safeLaunch {
                analytics.trackOperation(operation = TrainingsOperation.RefreshTrainings) {
                    trainingFeature.getTrainings(
                        start = range.from,
                        end = range.to,
                    )
                }
            }

            try {
                refresh.join()
            } finally {
                // A handled request failure must leave this range subscription alive.
                withContext(NonCancellable) {
                    refresh.cancelAndJoin()
                }
            }
        }
    }

    private fun provideTrainings(value: Pair<DateRange, List<Training>>) {
        val (range, trainings) = value

        val digest = trainingDigestUseCase.digest(trainings = trainings, range = range)

        val trainingTimeline = trainingTimelineUseCase.trainingTimeline(
            trainings = trainings,
            range = range,
            digest = digest,
        )

        val timeline = trainingTimeline.toState()

        val period = when (state.value.period) {
            is TrainingsTimelinePeriod.Daily -> {
                val items = timeline
                    .filterIsInstance<TimelineState.Daily.Item>()
                    .toPersistentList()

                TrainingsTimelinePeriod.Daily(items = items)
            }

            is TrainingsTimelinePeriod.Monthly -> TrainingsTimelinePeriod.Monthly.from(
                range = range,
                timeline = timeline,
            )
        }

        update { it.copy(period = period) }
    }

    private fun applyAnchor(date: LocalDateTime) {
        update { current ->
            val range = current.period.rangeFor(date).coerceWithin(current.limitations)
            if (current.date == range) {
                return@update current
            }

            current.selectRange(range)
        }
    }

    private fun openTrainingOverview(id: String) {
        val config = DialogConfig.Statistics.Training(
            title = UiText.Res(value = Res.string.statistics),
            id = id,
        )

        dialogController.open(config)
    }

    private fun deleteTraining(id: String) {
        safeLaunch {
            analytics.trackOperation(operation = TrainingsOperation.DeleteTraining) {
                deleteTrainingUseCase.execute(id)
            }
        }
    }

    private fun shiftDate(direction: Int) {
        if (direction == 0) {
            return
        }

        val current = state.value

        val shiftedRange = when (current.period) {
            is TrainingsTimelinePeriod.Monthly -> {
                val anchorDate = current.date.from.date.plus(DatePeriod(months = direction))
                val anchor = DateTimeUtils.startOfDay(anchorDate)
                current.period.rangeFor(anchor)
            }

            is TrainingsTimelinePeriod.Daily -> {
                val days = current.date.from.date.daysUntil(current.date.to.date) + 1
                val span = if (days <= 0) {
                    1
                } else {
                    days
                }
                val shiftPeriod = DatePeriod(days = span * direction)
                DateTimeUtils.shift(range = current.date, period = shiftPeriod)
            }
        }

        val range = shiftedRange.coerceWithin(current.limitations)
        if (range == current.date) {
            return
        }

        val event = if (direction > 0) {
            TrainingsEvent.NextDateSelected
        } else {
            TrainingsEvent.PreviousDateSelected
        }
        analytics.trackEvent(event = event)

        update { current ->
            current.selectRange(range)
        }
    }

    private fun openTrainingEdit(id: String) {
        val direction = EditTraining(id = id)
        navigateTo(direction)
    }

    override fun onTrainingMenuClick(id: String) {
        analytics.trackEvent(event = TrainingsEvent.TrainingMenuClicked)

        val dialog = DialogConfig.TrainingMenuPicker(
            title = UiText.Res(value = Res.string.menu),
            onResult = { item ->
                when (item) {
                    TrainingMenu.Delete -> {
                        analytics.trackEvent(event = TrainingsEvent.DeleteSelected)
                        deleteTraining(id)
                    }

                    TrainingMenu.Edit -> {
                        analytics.trackEvent(event = TrainingsEvent.EditSelected)
                        openTrainingEdit(id)
                    }

                    TrainingMenu.Details -> {
                        analytics.trackEvent(event = TrainingsEvent.DetailsSelected)
                        openTrainingOverview(id)
                    }
                }
            },
        )

        dialogController.open(dialog)
    }

    override fun onExerciseClick(id: String) {
        analytics.trackEvent(event = TrainingsEvent.ExerciseClicked)

        val dialog = DialogConfig.Exercise(
            title = UiText.Res(value = Res.string.exercise_record),
            id = id,
        )

        dialogController.open(dialog)
    }

    override fun onDailyDigestViewStats() {
        analytics.trackEvent(event = TrainingsEvent.DigestClicked)

        val current = state.value

        val range = current.period.effectiveRange(
            anchor = current.date.from,
            limitations = current.limitations,
        )

        val formattedRange = DateRangeFormatState.of(range)

        val rangeLabel = formattedRange.label()

        val title = rangeLabel?.let { label ->
            val formatArgs = persistentListOf(label)
            UiText.Res(value = Res.string.value_statistics, formatArgs = formatArgs)
        } ?: UiText.Res(value = Res.string.statistics)

        val config = DialogConfig.Statistics.Trainings(
            title = title,
            range = range,
        )

        dialogController.open(config)
    }

    override fun onSelectPeriod(id: String) {
        val current = state.value
        if (current.period.id == id) {
            return
        }

        val target = TrainingsTimelinePeriod.variantById(id) ?: return

        val event = when (target) {
            is TrainingsTimelinePeriod.Daily -> TrainingsEvent.DailyPeriodSelected
            is TrainingsTimelinePeriod.Monthly -> TrainingsEvent.MonthlyPeriodSelected
        }
        analytics.trackEvent(event = event)

        val alignedRange = target.defaultRange().coerceWithin(current.limitations)

        update { current ->
            val selectedState = current.copy(period = target)

            selectedState.selectRange(alignedRange)
        }
    }

    override fun onOpenDateSelector() {
        val current = state.value

        val event = when (current.period) {
            is TrainingsTimelinePeriod.Daily -> TrainingsEvent.DatePickerClicked
            is TrainingsTimelinePeriod.Monthly -> TrainingsEvent.MonthPickerClicked
        }
        analytics.trackEvent(event = event)

        val dialog = when (current.period) {
            is TrainingsTimelinePeriod.Daily -> DialogConfig.DatePicker(
                title = UiText.Res(value = Res.string.select_date),
                initial = current.date.from,
                format = DateFormat.DateOnly.DateDdMmmYyyy,
                limitations = current.limitations,
                onResult = { date ->
                    analytics.trackEvent(event = TrainingsEvent.DateConfirmed)
                    applyAnchor(date)
                },
            )

            is TrainingsTimelinePeriod.Monthly -> DialogConfig.MonthPicker(
                title = UiText.Res(value = Res.string.select_month),
                initial = current.date.from,
                format = DateFormat.DateOnly.MmmYyyy,
                limitations = current.limitations,
                onResult = { date ->
                    analytics.trackEvent(event = TrainingsEvent.MonthConfirmed)
                    applyAnchor(date)
                },
            )
        }

        dialogController.open(dialog)
    }

    override fun onSelectNextDate() {
        shiftDate(1)
    }

    override fun onSelectPreviousDate() {
        shiftDate(-1)
    }

    override fun onOpenDaily(date: LocalDate) {
        analytics.trackEvent(event = TrainingsEvent.DailySelected)

        val period = TrainingsTimelinePeriod.Daily()

        val anchor = DateTimeUtils.startOfDay(date)

        val range = period.rangeFor(anchor)
            .coerceWithin(state.value.limitations)

        update { it.copy(period = period, date = range) }
    }

    override fun onBack() {
        analytics.trackEvent(event = TrainingsEvent.DismissRequested)
        navigateTo(Back)
    }
}
