package com.grippo.home.home

import com.grippo.analytics.home.HomeAnalytics
import com.grippo.analytics.home.models.HomeEvent
import com.grippo.analytics.home.models.HomeOperation
import com.grippo.analytics.home.models.HomeScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.local.settings.models.HomeWelcomeStatus
import com.grippo.data.features.api.local.settings.models.Range
import com.grippo.data.features.api.metrics.distribution.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.distribution.models.MuscleLoadSummary
import com.grippo.data.features.api.metrics.engagement.TrainingStreakUseCase
import com.grippo.data.features.api.metrics.performance.ExerciseSpotlightUseCase
import com.grippo.data.features.api.metrics.performance.PerformanceTrendUseCase
import com.grippo.data.features.api.metrics.performance.models.PerformanceReference
import com.grippo.data.features.api.metrics.profile.GoalFollowingUseCase
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_details_btn
import com.grippo.design.resources.provider.goal_details_title
import com.grippo.design.resources.provider.metric_overview_streak_title
import com.grippo.design.resources.provider.muscle_loading
import com.grippo.design.resources.provider.notification_weight_description
import com.grippo.design.resources.provider.notification_weight_title
import com.grippo.design.resources.provider.performance_trend
import com.grippo.design.resources.provider.period_picker_title
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.value_muscle_loading
import com.grippo.design.resources.provider.value_performance_trend
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.metrics.distribution.toState
import com.grippo.domain.state.metrics.engagement.toState
import com.grippo.domain.state.metrics.performance.toState
import com.grippo.domain.state.metrics.profile.toState
import com.grippo.domain.state.range.toState
import com.grippo.domain.state.training.toState
import com.grippo.domain.state.user.toState
import com.grippo.screen.api.deeplink.Deeplink
import com.grippo.state.domain.range.toDomain
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateRangeKind
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.date.utils.currentDateFlow
import com.grippo.toolkit.local.notification.AppNotification
import com.grippo.toolkit.local.notification.NotificationKey
import com.grippo.toolkit.local.notification.NotificationManager
import com.grippo.toolkit.permission.AppPermission
import com.grippo.toolkit.permission.PermissionManager
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.days

internal class HomeViewModel(
    private val analytics: HomeAnalytics,
    private val trainingFeature: TrainingFeature,
    private val localSettingsFeature: LocalSettingsFeature,
    private val dialogController: DialogController,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
    private val exerciseSpotlightUseCase: ExerciseSpotlightUseCase,
    private val trainingStreakUseCase: TrainingStreakUseCase,
    private val performanceTrendUseCase: PerformanceTrendUseCase,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val stringProvider: StringProvider,
    permissionManager: PermissionManager,
    private val notificationManager: NotificationManager,
    private val goalFollowingUseCase: GoalFollowingUseCase,
    userFeature: UserFeature,
    excludedMusclesFeature: ExcludedMusclesFeature,
    excludedEquipmentsFeature: ExcludedEquipmentsFeature,
    goalFeature: GoalFeature,
    muscleFeature: MuscleFeature,
    equipmentFeature: EquipmentFeature,
) : BaseViewModel<HomeState, HomeDirection, HomeLoader>(
    HomeState(range = DateRangeFormatState.Empty(kind = DateRangeKind.Last30Days))
), HomeContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(HomeScreen.Home, reason)
    }

    init {
        userFeature.observeUser()
            .onEach(::provideUser)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        excludedMusclesFeature.observeExcludedMuscles()
            .onEach(::provideExcludedMuscles)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        excludedEquipmentsFeature.observeExcludedEquipments()
            .onEach(::provideExcludedEquipments)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        goalFeature.observeGoal()
            .onEach(::provideGoalPresence)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        localSettingsFeature.observeRange()
            .combine(currentDateFlow()) { range, _ -> range }
            .onEach(::provideRange)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        safeLaunch(recovery = ErrorRecovery.Retry) {
            permissionManager.request(AppPermission.Notifications)
        }

        safeLaunch(recovery = ErrorRecovery.Retry) {
            scheduleWeightReminder()
        }

        safeLaunch(recovery = ErrorRecovery.Retry) {
            analytics.trackOperation(operation = HomeOperation.RefreshCatalogs) {
                muscleFeature.getMuscles()
                equipmentFeature.getEquipments()
                exerciseExampleFeature.getExerciseExamples()
            }
        }

        val selectedPeriods = state
            .mapNotNull { it.range.value }
            .distinctUntilChanged()

        val metricPeriods = selectedPeriods
            .combine(currentDateFlow()) { period, _ -> period }

        metricPeriods
            .flatMapLatest { period ->
                trainingFeature.observeTrainings(start = period.from, end = period.to)
                    .flatMapLatest { trainings ->
                        val ids = trainings.flatMap { it.exercises }.map { it.exerciseExample.id }.distinct()
                        exerciseExampleFeature.observeExerciseExamples(ids)
                            .map { examples ->
                                Triple(first = period, second = trainings, third = examples)
                            }
                    }
            }
            .onEach(::provideTrainings)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        metricPeriods
            .flatMapLatest { period ->
                trainingFeature.observeTrainings(start = period.from, end = period.to)
                    .flatMapLatest(muscleLoadingSummaryUseCase::observeFromTrainings)
            }
            .onEach(::provideMuscleLoad)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        metricPeriods
            .flatMapLatest { period ->
                combine(
                    trainingFeature.observeTrainings(start = period.from, end = period.to),
                    goalFeature.observeGoal(),
                ) { trainings, goal -> Triple(first = period, second = trainings, third = goal) }
                    .flatMapLatest { source ->
                        val ids = source.second.flatMap { it.exercises }.map { it.exerciseExample.id }.distinct()
                        exerciseExampleFeature.observeExerciseExamples(ids).map { source }
                    }
            }
            .mapLatest(::provideGoalProgress)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        selectedPeriods
            .mapLatest(::refreshTrainings)
            .safeLaunch(recovery = ErrorRecovery.Retry)

        safeLaunch(recovery = ErrorRecovery.Retry) {
            val status = localSettingsFeature.observeHomeWelcomeStatus().first()
            provideWelcomeStatus(status)
        }
    }

    private suspend fun scheduleWeightReminder() {
        val key = NotificationKey.ChangeWeight
        if (notificationManager.isPending(key)) {
            return
        }

        val notification = AppNotification(
            id = key,
            title = stringProvider.get(Res.string.notification_weight_title),
            body = stringProvider.get(Res.string.notification_weight_description),
            deeplink = Deeplink.WeightHistory.key,
        )

        notificationManager.show(notification = notification, delay = 7.days)
    }

    private suspend fun provideWelcomeStatus(value: HomeWelcomeStatus) {
        if (value != HomeWelcomeStatus.PendingCelebration) {
            return
        }

        localSettingsFeature.setHomeWelcomeStatus(HomeWelcomeStatus.Celebrated)

        update { it.copy(showWelcomeConfetti = true) }
    }

    private suspend fun refreshTrainings(period: DateRange) {
        withLoader(HomeLoader.Trainings) {
            analytics.trackOperation(operation = HomeOperation.RefreshTrainings) {
                trainingFeature.getTrainings(start = period.from, end = period.to)
            }
        }
    }

    private fun provideUser(value: User?) {
        val user = value?.toState() ?: return

        update { it.copy(user = user) }
    }

    private fun provideExcludedMuscles(value: List<Muscle>) {
        val count = value.size

        update { it.copy(excludedMusclesCount = count) }
    }

    private fun provideExcludedEquipments(value: List<Equipment>) {
        val count = value.size

        update { it.copy(missingEquipmentCount = count) }
    }

    private fun provideGoalPresence(value: Goal?) {
        val hasGoal = value != null

        update { it.copy(hasGoal = hasGoal) }
    }

    private fun provideRange(value: Range) {
        val kind = value.toState()

        val range = DateRangeFormatState.of(kind)

        update { it.copy(range = range) }
    }

    private fun provideTrainings(value: Triple<DateRange, List<Training>, List<ExerciseExample>>) {
        val (period, list, examples) = value

        val performance = performanceTrendUseCase
            .fromTrainings(trainings = list, reference = PerformanceReference.Average)
            .toState()

        val last = list.maxByOrNull { it.createdAt }?.toState()

        val streak = list.takeIf { it.isNotEmpty() }?.let { trainings ->
            trainingStreakUseCase
                .fromTrainings(trainings = trainings, range = period)
                .toState()
        }

        val spotlights = exerciseSpotlightUseCase
            .buildSpotlights(
                trainings = list,
                examples = examples,
                evaluatedAt = minOf(DateTimeUtils.now(), period.to),
                historyStart = period.from.date,
            )
            .toState()

        update {
            it.copy(
                spotlights = spotlights,
                streak = streak,
                performance = performance,
                lastTraining = last,
            )
        }
    }

    private suspend fun provideGoalProgress(value: Triple<DateRange, List<Training>, Goal?>) {
        val (period, trainings, goal) = value
        val now = DateTimeUtils.now()
        val adherence = goal?.let {
            goalFollowingUseCase.fromTrainingsByPrimary(
                trainings = trainings,
                goal = it,
                range = period,
                now = now,
            )
        }
        val goalProgress = adherence?.toState(now = now)

        update { it.copy(goalProgress = goalProgress) }
    }

    private fun provideMuscleLoad(value: MuscleLoadSummary) {
        val muscleLoad = value.takeIf { it.meta.trainingsCount > 0 }?.toState()

        update { it.copy(muscleLoad = muscleLoad) }
    }

    override fun onPerformanceMetricClick(type: PerformanceMetricTypeState) {
        val range = state.value.range.value ?: return
        analytics.trackEvent(HomeEvent.PerformanceMetricClicked)

        val rangeLabel = state.value.range.label()
        val metricLabel = type.labelText()
        val title = rangeLabel?.let {
            UiText.Res(value = Res.string.value_performance_trend, formatArgs = persistentListOf(it, metricLabel))
        } ?: UiText.Res(value = Res.string.performance_trend, formatArgs = persistentListOf(metricLabel))
        val dialog = DialogConfig.PerformanceTrendDetails(
            title = title,
            range = range,
            metricType = type,
            reference = PerformanceReferenceState.Average,
        )

        dialogController.open(dialog)
    }

    override fun onOpenMuscleLoading() {
        val range = state.value.range.value ?: return
        analytics.trackEvent(HomeEvent.MuscleLoadingClicked)

        val rangeLabel = state.value.range.label()
        val title = rangeLabel?.let {
            UiText.Res(value = Res.string.value_muscle_loading, formatArgs = persistentListOf(it))
        } ?: UiText.Res(Res.string.muscle_loading)
        val dialog = DialogConfig.MuscleLoadingDetails(
            title = title,
            range = range,
        )
        dialogController.open(dialog)
    }

    override fun onOpenTrainings() {
        analytics.trackEvent(HomeEvent.TrainingsClicked)
        navigateTo(HomeDirection.Trainings)
    }

    override fun onOpenPeriodPicker() {
        analytics.trackEvent(HomeEvent.PeriodPickerClicked)
        val dialog = DialogConfig.PeriodPicker(
            title = UiText.Res(Res.string.period_picker_title),
            initial = state.value.range.kind,
            onResult = { result ->
                analytics.trackEvent(event = HomeEvent.PeriodConfirmed)
                safeLaunch(recovery = ErrorRecovery.Retry) {
                    analytics.trackOperation(operation = HomeOperation.SavePeriod) {
                        val range = result.toDomain()
                        localSettingsFeature.setRange(range)
                    }
                }
            }
        )

        dialogController.open(dialog)
    }

    override fun onOpenExample(id: String) {
        analytics.trackEvent(HomeEvent.ExerciseClicked)
        val dialog = DialogConfig.ExerciseExampleDetails(
            title = UiText.Res(Res.string.exercise_details_btn),
            id = id
        )

        dialogController.open(dialog)
    }

    override fun onOpenTrainingStreak() {
        val range = state.value.range.value ?: return
        analytics.trackEvent(HomeEvent.TrainingStreakClicked)

        val dialog = DialogConfig.TrainingStreakDetails(
            title = UiText.Res(Res.string.metric_overview_streak_title),
            range = range
        )

        dialogController.open(dialog)
    }

    override fun onOpenGoalDetails() {
        val range = state.value.range.value ?: return
        analytics.trackEvent(HomeEvent.GoalDetailsClicked)

        val dialog = DialogConfig.TrainingGoalDetails(
            title = UiText.Res(Res.string.goal_details_title),
            range = range,
            onAddGoal = {
                analytics.trackEvent(HomeEvent.AddGoalClicked)
                navigateTo(HomeDirection.Goal)
            }
        )

        dialogController.open(dialog)
    }

    override fun onAddGoal() {
        analytics.trackEvent(HomeEvent.AddGoalClicked)
        navigateTo(HomeDirection.Goal)
    }

    override fun onWelcomeConfettiShown() {
        update { it.copy(showWelcomeConfetti = false) }
    }

    override fun onBack() {
        analytics.trackEvent(HomeEvent.DismissRequested)
        navigateTo(HomeDirection.Back)
    }
}
