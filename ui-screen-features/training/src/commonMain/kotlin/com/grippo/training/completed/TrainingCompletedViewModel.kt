package com.grippo.training.completed

import androidx.compose.ui.graphics.ImageBitmap
import com.grippo.analytics.training.TrainingAnalytics
import com.grippo.analytics.training.models.TrainingEvent
import com.grippo.analytics.training.models.TrainingOperation
import com.grippo.analytics.training.models.TrainingScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.metrics.engagement.TrainingStreakState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.core.state.metrics.performance.PerformanceVerdictState
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.local.settings.models.Range
import com.grippo.data.features.api.metrics.distribution.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.engagement.TrainingStreakUseCase
import com.grippo.data.features.api.metrics.performance.PerformanceTrendUseCase
import com.grippo.data.features.api.metrics.performance.TrainingRecordsUseCase
import com.grippo.data.features.api.metrics.performance.models.TrainingRecord
import com.grippo.data.features.api.metrics.profile.GoalFollowingUseCase
import com.grippo.data.features.api.metrics.volume.TrainingTotalUseCase
import com.grippo.data.features.api.metrics.volume.WeeklyVolumeUseCase
import com.grippo.data.features.api.training.SetTrainingUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.UpdateTrainingUseCase
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_details_btn
import com.grippo.design.resources.provider.goal_details_title
import com.grippo.design.resources.provider.metric_overview_streak_title
import com.grippo.design.resources.provider.performance_trend
import com.grippo.design.resources.provider.value_performance_trend
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.metrics.distribution.toState
import com.grippo.domain.state.metrics.engagement.toState
import com.grippo.domain.state.metrics.performance.toState
import com.grippo.domain.state.metrics.profile.toState
import com.grippo.domain.state.metrics.volume.toState
import com.grippo.domain.state.range.toState
import com.grippo.domain.state.training.toState
import com.grippo.state.domain.metrics.performance.toDomain
import com.grippo.state.domain.training.toDomain
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.date.utils.currentDateFlow
import com.grippo.toolkit.share.ImageSharer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.datetime.LocalDateTime

internal class TrainingCompletedViewModel(
    private val stage: StageState,
    exercises: List<ExerciseState>,
    startAt: LocalDateTime,
    private val analytics: TrainingAnalytics,
    private val trainingFeature: TrainingFeature,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    localSettingsFeature: LocalSettingsFeature,
    private val goalFeature: GoalFeature,
    private val setTrainingUseCase: SetTrainingUseCase,
    private val updateTrainingUseCase: UpdateTrainingUseCase,
    private val trainingTotalUseCase: TrainingTotalUseCase,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
    private val performanceTrendUseCase: PerformanceTrendUseCase,
    private val trainingRecordsUseCase: TrainingRecordsUseCase,
    private val weeklyVolumeUseCase: WeeklyVolumeUseCase,
    private val trainingStreakUseCase: TrainingStreakUseCase,
    private val goalFollowingUseCase: GoalFollowingUseCase,
    private val dialogController: DialogController,
    private val imageSharer: ImageSharer,
    initialState: TrainingCompletedState,
) : BaseViewModel<TrainingCompletedState, TrainingCompletedDirection, TrainingCompletedLoader>(
    state = initialState,
), TrainingCompletedContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(TrainingScreen.Completed, reason)
    }

    init {
        val savedIds = state
            .map { it.savedTrainingId }
            .distinctUntilChanged()
            .filterNotNull()

        val savedTrainings = savedIds
            .flatMapLatest(trainingFeature::observeTraining)

        savedTrainings
            .onEach(::provideTraining)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        savedTrainings
            .filterNotNull()
            .take(1)
            .mapLatest(::loadRecords)
            .safeLaunch(processing = Processing.Infinity, recovery = ErrorRecovery.Retry)

        savedIds
            .take(1)
            .mapLatest { refreshExamples() }
            .safeLaunch(processing = Processing.Infinity, recovery = ErrorRecovery.Retry)

        localSettingsFeature.observeRange()
            .combine(currentDateFlow()) { range, _ -> range }
            .onEach(::provideRange)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        savedIds
            .combine(currentDateFlow()) { _, _ -> DateTimeUtils.now() }
            .flatMapLatest(::observeWeeklyTrainings)
            .onEach(::provideWeeklyVolume)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        val savedPeriods = savedIds
            .flatMapLatest { observePeriods() }

        val comparisonPeriods = savedTrainings
            .filterNotNull()
            .flatMapLatest(::observeComparisonPeriods)

        combine(
            savedPeriods,
            comparisonPeriods,
            observeReference(),
            savedTrainings.filterNotNull(),
        ) { period, comparisonPeriod, reference, saved ->
            PeriodReference(saved = saved, period = period, comparisonPeriod = comparisonPeriod, reference = reference)
        }
            .distinctUntilChanged()
            .flatMapLatest(::observePeriodTrainings)
            .onEach(::provideTrainings)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        savedPeriods
            .flatMapLatest(::observePeriodGoal)
            .mapLatest(::provideGoalProgress)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        savedPeriods
            .combine(comparisonPeriods.mapNotNull { it.value }) { period, comparisonPeriod ->
                RefreshPeriods(period = period, comparisonPeriod = comparisonPeriod)
            }
            .distinctUntilChanged()
            .mapLatest(::refreshTrainings)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        safeLaunch(
            loader = TrainingCompletedLoader.SaveTraining,
            recovery = ErrorRecovery.Close(onClose = { navigateTo(TrainingCompletedDirection.Back) }),
        ) {
            val id = state.value.savedTrainingId ?: saveTraining(
                exercises = exercises,
                startAt = startAt,
            )

            analytics.trackOperation(operation = TrainingOperation.DeleteDraft) {
                trainingFeature.deleteDraftTraining()
            }

            val training = analytics.trackOperation(operation = TrainingOperation.LoadTraining) {
                trainingFeature.observeTraining(id).firstOrNull()
            }
            provideTraining(training)
        }
    }

    private suspend fun saveTraining(
        exercises: List<ExerciseState>,
        startAt: LocalDateTime,
    ): String {
        val training = assembleTraining(
            exercises = exercises,
            startAt = startAt,
        )

        val savedId = when (val trainingId = stage.id) {
            null -> {
                analytics.trackOperation(operation = TrainingOperation.CreateTraining) {
                    setTrainingUseCase.execute(training)
                }
            }

            else -> {
                analytics.trackOperation(operation = TrainingOperation.UpdateTraining) {
                    updateTrainingUseCase.execute(id = trainingId, training = training)
                }
            }
        }

        update { it.copy(savedTrainingId = savedId) }
        return savedId
    }

    private fun assembleTraining(
        exercises: List<ExerciseState>,
        startAt: LocalDateTime,
    ): SetTraining {
        val duration = DateTimeUtils.ago(value = startAt)

        val domainExercises = exercises.toDomain()

        val totals = trainingTotalUseCase.fromSetExercises(domainExercises)

        return SetTraining(
            exercises = domainExercises,
            duration = duration,
            volume = totals.volume,
            intensity = totals.intensity,
            repetitions = totals.repetitions,
        )
    }

    private fun observePeriods(): Flow<DateRange> {
        return state
            .map { it.range.value }
            .filterNotNull()
            .distinctUntilChanged()
    }

    private fun observeComparisonPeriods(saved: Training): Flow<DateRangeFormatState> {
        return state
            .map { it.range.kind }
            .distinctUntilChanged()
            .map { kind -> DateRangeFormatState.of(kind = kind, anchor = saved.createdAt.date) }
    }

    private fun observeReference(): Flow<PerformanceReferenceState> {
        return state
            .map { it.reference }
            .distinctUntilChanged()
    }

    private fun observePeriodTrainings(value: PeriodReference): Flow<PeriodTrainings> {
        val comparisonRange = value.comparisonPeriod.value ?: return emptyFlow()

        val periodTrainings = trainingFeature.observeTrainings(start = value.period.from, end = value.period.to)

        val comparisonTrainings = trainingFeature.observeTrainings(
            start = comparisonRange.from,
            end = comparisonRange.to,
        )

        return combine(periodTrainings, comparisonTrainings) { trainings, comparison ->
            PeriodTrainings(source = value, periodTrainings = trainings, comparisonTrainings = comparison)
        }
    }

    private fun observePeriodGoal(period: DateRange): Flow<PeriodGoal> {
        val periodTrainings = trainingFeature.observeTrainings(start = period.from, end = period.to)

        val goals = goalFeature.observeGoal()

        return combine(periodTrainings, goals) { trainings, goal ->
            PeriodGoal(period = period, trainings = trainings, goal = goal)
        }.flatMapLatest { source ->
            observeExamples(source.trainings).map { source }
        }
    }

    private fun observeExamples(trainings: List<Training>): Flow<List<ExerciseExample>> {
        val ids = trainings.flatMap { it.exercises }.map { it.exerciseExample.id }.distinct()

        return exerciseExampleFeature.observeExerciseExamples(ids)
    }

    private fun observeWeeklyTrainings(now: LocalDateTime): Flow<WeeklyTrainings> {
        val window = weeklyVolumeUseCase.observationRange(now)

        return trainingFeature.observeTrainings(start = window.from, end = window.to)
            .map { trainings -> WeeklyTrainings(now = now, trainings = trainings) }
    }

    private suspend fun refreshTrainings(value: RefreshPeriods) {
        val (period, comparisonPeriod) = value

        val window = weeklyVolumeUseCase.observationRange(DateTimeUtils.now())

        val start = minOf(period.from, comparisonPeriod.from, window.from)

        val end = maxOf(period.to, comparisonPeriod.to, window.to)

        analytics.trackOperation(operation = TrainingOperation.RefreshTrainings) {
            trainingFeature.getTrainings(start = start, end = end)
        }
    }

    private suspend fun refreshExamples() {
        analytics.trackOperation(operation = TrainingOperation.RefreshExamples) {
            exerciseExampleFeature.getExerciseExamples()
        }
    }

    private suspend fun loadRecords(training: Training) {
        val records = withLoader(loader = TrainingCompletedLoader.LoadRecords) {
            analytics.trackOperation(operation = TrainingOperation.LoadRecords) {
                trainingRecordsUseCase.fromTraining(training)
            }
        }

        provideRecords(records)
    }

    private suspend fun provideTraining(value: Training?) {
        value ?: return

        val muscleLoad = muscleLoadingSummaryUseCase
            .fromTraining(value)
            .toState()

        val training = value.toState()

        update { it.copy(training = training, muscleLoad = muscleLoad) }
    }

    private fun provideRange(value: Range) {
        val kind = value.toState()

        val range = DateRangeFormatState.of(kind)

        update { it.copy(range = range) }
    }

    private fun provideTrainings(value: PeriodTrainings) {
        val (source, periodTrainings, comparisonTrainings) = value

        val (saved, period, comparisonPeriod, reference) = source

        val domainReference = reference.toDomain()

        val metrics = performanceTrendUseCase
            .fromTraining(training = saved, periodTrainings = comparisonTrainings, reference = domainReference)
            .toState()

        val performance = PerformanceComparisonState(metrics = metrics, reference = reference, period = comparisonPeriod)

        val verdict = PerformanceVerdictState.of(metrics)

        val isPeriodRecord = verdict == PerformanceVerdictState.Record && performance.isOver(period)

        val now = evaluationTime(periodTrainings)

        val streak = streakOf(trainings = periodTrainings, period = period, now = now)

        val previousStreak = if (stage.id == null) {
            val previousTrainings = periodTrainings.filter { it.id != saved.id }

            streakOf(trainings = previousTrainings, period = period, now = now)
        } else {
            null
        }

        update { current ->
            val recordCelebration = current.recordCelebration.celebrate(isRecord = isPeriodRecord)

            current.copy(
                performance = performance,
                streak = streak,
                previousStreak = previousStreak,
                recordCelebration = recordCelebration,
            )
        }
    }

    private fun provideWeeklyVolume(value: WeeklyTrainings) {
        val weeklyVolume = weeklyVolumeUseCase
            .fromTrainings(trainings = value.trainings, now = value.now)
            .toState()

        update { it.copy(weeklyVolume = weeklyVolume) }
    }

    private fun provideRecords(value: List<TrainingRecord>) {
        val records = value.toState()

        update { current ->
            val recordCelebration = current.recordCelebration.celebrate(isRecord = records.isNotEmpty())

            current.copy(records = records, recordCelebration = recordCelebration)
        }
    }

    private suspend fun provideGoalProgress(value: PeriodGoal) {
        val (period, trainings, goal) = value

        val savedId = state.value.savedTrainingId

        val now = evaluationTime(trainings)

        val goalProgress = goalProgressOf(
            trainings = trainings,
            goal = goal,
            period = period,
            now = now,
        )

        val previousGoalProgress = if (stage.id == null) {
            val previousTrainings = trainings.filter { it.id != savedId }

            goalProgressOf(
                trainings = previousTrainings,
                goal = goal,
                period = period,
                now = now,
            )
        } else {
            null
        }

        update { it.copy(goalProgress = goalProgress, previousGoalProgress = previousGoalProgress) }
    }

    // Saved createdAt is server time; take the later of it and now so a lagging device clock keeps the workout.
    private fun evaluationTime(trainings: List<Training>): LocalDateTime {
        val now = DateTimeUtils.now()

        val savedId = state.value.savedTrainingId

        val savedAt = trainings.firstOrNull { it.id == savedId }?.createdAt ?: return now

        return now.coerceAtLeast(savedAt)
    }

    private fun streakOf(
        trainings: List<Training>,
        period: DateRange,
        now: LocalDateTime,
    ): TrainingStreakState? {
        if (trainings.isEmpty()) return null

        return trainingStreakUseCase
            .fromTrainings(trainings = trainings, range = period, now = now)
            .toState()
    }

    private suspend fun goalProgressOf(
        trainings: List<Training>,
        goal: Goal?,
        period: DateRange,
        now: LocalDateTime,
    ): GoalProgressState? {
        goal ?: return null
        if (trainings.isEmpty()) return null

        val adherence = goalFollowingUseCase.fromTrainingsByPrimary(
            trainings = trainings,
            goal = goal,
            range = period,
            now = now,
        )

        return adherence.toState(now = now)
    }

    private fun trendRange(period: DateRange): DateRange {
        val savedAt = state.value.training?.createdAt?.value
        if (stage.id == null || savedAt == null) {
            return period
        }

        return period.copy(to = DateTimeUtils.endOfDay(savedAt))
    }

    override fun onShareClick(capture: suspend () -> ImageBitmap) {
        if (TrainingCompletedLoader.ShareRecap in loaders.value) return

        analytics.trackEvent(event = TrainingEvent.Completed.ShareClicked)

        safeLaunch(loader = TrainingCompletedLoader.ShareRecap, recovery = ErrorRecovery.Retry) {
            analytics.trackOperation(operation = TrainingOperation.ShareRecap) {
                val image = capture()
                imageSharer.share(image = image, fileName = SHARE_FILE_NAME)
            }
        }
    }

    override fun onRecordClick(exampleId: String) {
        analytics.trackEvent(event = TrainingEvent.Completed.RecordOpened)

        val dialog = DialogConfig.ExerciseExampleDetails(
            title = UiText.Res(value = Res.string.exercise_details_btn),
            id = exampleId,
        )

        dialogController.open(dialog)
    }

    override fun onReferenceSelect(reference: PerformanceReferenceState) {
        if (state.value.reference == reference) return

        analytics.trackEvent(event = TrainingEvent.Completed.ReferenceSelected)

        update { it.copy(reference = reference) }
    }

    override fun onMetricClick(type: PerformanceMetricTypeState) {
        val performance = state.value.performance ?: return

        val period = performance.period.value ?: return

        analytics.trackEvent(event = TrainingEvent.Completed.MetricOpened)

        val range = trendRange(period)

        val rangeLabel = state.value.range.takeIf { it.value == range }?.label()

        val metricLabel = type.labelText()

        val title = if (rangeLabel == null) {
            UiText.Res(value = Res.string.performance_trend, formatArgs = persistentListOf(metricLabel))
        } else {
            UiText.Res(
                value = Res.string.value_performance_trend,
                formatArgs = persistentListOf(rangeLabel, metricLabel),
            )
        }

        val dialog = DialogConfig.PerformanceTrendDetails(
            title = title,
            range = range,
            metricType = type,
            reference = performance.reference,
        )

        dialogController.open(dialog)
    }

    override fun onStreakClick() {
        val range = state.value.range.value ?: return

        analytics.trackEvent(event = TrainingEvent.Completed.StreakOpened)

        val dialog = DialogConfig.TrainingStreakDetails(
            title = UiText.Res(value = Res.string.metric_overview_streak_title),
            range = range,
        )

        dialogController.open(dialog)
    }

    override fun onGoalClick() {
        val range = state.value.range.value ?: return

        analytics.trackEvent(event = TrainingEvent.Completed.GoalOpened)

        val dialog = DialogConfig.TrainingGoalDetails(
            title = UiText.Res(value = Res.string.goal_details_title),
            range = range,
            onAddGoal = {
                analytics.trackEvent(event = TrainingEvent.Completed.AddGoalClicked)
                navigateTo(TrainingCompletedDirection.Goal)
            },
        )

        dialogController.open(dialog)
    }

    override fun onRecordConfettiShown() {
        update { it.copy(recordCelebration = RecordCelebrationState.Done) }
    }

    override fun onBack() {
        if (TrainingCompletedLoader.SaveTraining in loaders.value) return

        analytics.trackEvent(event = TrainingEvent.Completed.DismissRequested)
        navigateTo(TrainingCompletedDirection.Back)
    }

    private data class PeriodReference(
        val saved: Training,
        val period: DateRange,
        val comparisonPeriod: DateRangeFormatState,
        val reference: PerformanceReferenceState,
    )

    private data class PeriodTrainings(
        val source: PeriodReference,
        val periodTrainings: List<Training>,
        val comparisonTrainings: List<Training>,
    )

    private data class RefreshPeriods(
        val period: DateRange,
        val comparisonPeriod: DateRange,
    )

    private data class PeriodGoal(
        val period: DateRange,
        val trainings: List<Training>,
        val goal: Goal?,
    )

    private data class WeeklyTrainings(
        val now: LocalDateTime,
        val trainings: List<Training>,
    )

    private companion object {
        const val SHARE_FILE_NAME = "grippo-workout.png"
    }
}
