package com.grippo.home.home

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.stage.TrainingSeed
import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.goal.GoalSetupSuggestionUseCase
import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.local.settings.models.Range
import com.grippo.data.features.api.metrics.distribution.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.engagement.TrainingStreakUseCase
import com.grippo.data.features.api.metrics.performance.ExerciseSpotlightUseCase
import com.grippo.data.features.api.metrics.performance.PerformanceTrendUseCase
import com.grippo.data.features.api.metrics.profile.GoalFollowingUseCase
import com.grippo.data.features.api.metrics.profile.TrainingLoadProfileUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetDraftTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.notification_weight_description
import com.grippo.design.resources.provider.notification_weight_title
import com.grippo.design.resources.provider.period_picker_title
import com.grippo.design.resources.provider.providers.StringProvider
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
import com.grippo.toolkit.local.notification.AppNotification
import com.grippo.toolkit.local.notification.NotificationKey
import com.grippo.toolkit.local.notification.NotificationManager
import com.grippo.toolkit.permission.AppPermission
import com.grippo.toolkit.permission.PermissionManager
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.days

internal class HomeViewModel(
    private val trainingFeature: TrainingFeature,
    private val localSettingsFeature: LocalSettingsFeature,
    private val dialogController: DialogController,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
    private val exerciseSpotlightUseCase: ExerciseSpotlightUseCase,
    private val trainingStreakUseCase: TrainingStreakUseCase,
    private val performanceTrendUseCase: PerformanceTrendUseCase,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val trainingLoadProfileUseCase: TrainingLoadProfileUseCase,
    private val stringProvider: StringProvider,
    private val permissionManager: PermissionManager,
    private val notificationManager: NotificationManager,
    private val goalFollowingUseCase: GoalFollowingUseCase,
    private val goalSetupSuggestionUseCase: GoalSetupSuggestionUseCase,
    userFeature: UserFeature,
    excludedMusclesFeature: ExcludedMusclesFeature,
    excludedEquipmentsFeature: ExcludedEquipmentsFeature,
) : BaseViewModel<HomeState, HomeDirection, HomeLoader>(
    HomeState()
), HomeContract {

    init {
        userFeature
            .observeUser()
            .onEach(::provideUser)
            .safeLaunch()

        excludedMusclesFeature
            .observeExcludedMuscles()
            .onEach { update { state -> state.copy(excludedMusclesCount = it.size) } }
            .safeLaunch()

        excludedEquipmentsFeature
            .observeExcludedEquipments()
            .onEach { update { state -> state.copy(missingEquipmentCount = it.size) } }
            .safeLaunch()

        localSettingsFeature.observeRange()
            .onEach(::provideRange)
            .safeLaunch()

        safeLaunch {
            permissionManager.request(AppPermission.Notifications)
        }

        safeLaunch {
            val weightNotificationKey = NotificationKey.ChangeWeight

            if (notificationManager.isPending(weightNotificationKey).not()) {
                val notification = AppNotification(
                    id = weightNotificationKey,
                    title = stringProvider.get(Res.string.notification_weight_title),
                    body = stringProvider.get(Res.string.notification_weight_description),
                    deeplink = Deeplink.WeightHistory.key
                )
                notificationManager.show(notification, 3.days)
            }
        }

        state
            .map { it.range.value }
            .filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { period ->
                trainingFeature
                    .observeTrainings(start = period.from, end = period.to)
                    .onEach(::provideTrainings)
            }.safeLaunch()

        state
            .map { it.range.value }
            .filterNotNull()
            .distinctUntilChanged()
            .mapLatest { period ->
                withLoader(HomeLoader.Trainings) {
                    trainingFeature
                        .getTrainings(start = period.from, end = period.to)
                }
            }
            .safeLaunch()

        safeLaunch {
            exerciseExampleFeature.getExerciseExamples()
        }

        trainingFeature.getDraftTraining()
            .onEach(::provideDraftTraining)
            .safeLaunch()
    }

    private fun provideUser(value: User?) {
        val user = value?.toState() ?: return
        update { it.copy(user = user) }
    }

    private fun provideRange(value: Range?) {
        val kind = value?.toState() ?: return
        update { it.copy(range = DateRangeFormatState.of(kind)) }
    }

    private fun provideDraftTraining(value: SetDraftTraining?) {
        val hasDraftTraining = value != null
        update { it.copy(hasDraftTraining = hasDraftTraining) }
    }

    private suspend fun provideTrainings(list: List<Training>) {
        if (list.isEmpty()) {
            clearHome()
            return
        }

        val sortedTrainingsDesc = list.sortedByDescending { it.createdAt }

        val trainings = sortedTrainingsDesc.toState()

        val last = trainings.firstOrNull() ?: return

        val totalDuration = list.fold(ZERO) { acc: Duration, item ->
            acc + item.duration
        }

        val streak = trainingStreakUseCase
            .fromTrainings(list)
            .toState()

        val spotlights = exerciseSpotlightUseCase
            .buildSpotlights(list)
            .toState()

        val performance = performanceTrendUseCase
            .fromTrainings(list)
            .toState()

        val muscleLoadSummary = muscleLoadingSummaryUseCase
            .fromTrainings(list)
            .toState()

        val profile = trainingLoadProfileUseCase
            .fromTrainings(list)
            .toState()

        val goalProgress = goalFollowingUseCase
            .fromTrainingsByPrimary(list)
            ?.toState()

        update {
            it.copy(
                totalDuration = totalDuration,
                spotlights = spotlights,
                muscleLoad = muscleLoadSummary,
                streak = streak,
                performance = performance,
                lastTraining = last,
                goalProgress = goalProgress,
                profile = profile
            )
        }
    }

    override fun onStartTraining() {
        safeLaunch {
            if (goalSetupSuggestionUseCase.shouldSuggest()) {
                goalSetupSuggestionUseCase.markShown()
                val config = DialogConfig.GoalSetupSuggestion(
                    onConfigure = { navigateTo(HomeDirection.Goal) },
                    onLater = { navigateTo(HomeDirection.StartTraining(TrainingSeed.Blank)) },
                )
                dialogController.show(config)
            } else {
                navigateTo(HomeDirection.StartTraining(TrainingSeed.Blank))
            }
        }
    }

    override fun onPerformanceMetricClick(type: PerformanceMetricTypeState) {
        val range = state.value.range.value ?: return

        val dialog = DialogConfig.PerformanceTrendDetails(
            range = range,
            metricType = type,
        )

        dialogController.show(dialog)
    }

    override fun onOpenMuscleLoading() {
        val range = state.value.range.value ?: return

        val dialog = DialogConfig.MuscleLoadingDetails(
            range = range,
        )
        dialogController.show(dialog)
    }

    override fun onOpenProfile() {
        val dialog = DialogConfig.Profile(
            onProfileResult = {
                when (it) {
                    ProfileMenu.Muscles -> navigateTo(HomeDirection.ExcludedMuscles)
                    ProfileMenu.Equipment -> navigateTo(HomeDirection.MissingEquipment)
                    ProfileMenu.Experience -> navigateTo(HomeDirection.Experience)
                    ProfileMenu.Body -> navigateTo(HomeDirection.Body)
                    ProfileMenu.Goal -> navigateTo(HomeDirection.Goal)
                }
            },
            onSettingsResult = {
                when (it) {
                    SettingsMenu.Debug -> navigateTo(HomeDirection.Debug)
                    SettingsMenu.Settings -> navigateTo(HomeDirection.Settings)
                    SettingsMenu.Social -> navigateTo(HomeDirection.Social)
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onOpenTrainings() {
        navigateTo(HomeDirection.Trainings)
    }

    override fun onOpenPeriodPicker() {
        safeLaunch {
            val dialog = DialogConfig.PeriodPicker(
                title = stringProvider.get(Res.string.period_picker_title),
                initial = state.value.range.kind,
                onResult = { result ->
                    safeLaunch {
                        localSettingsFeature.setRange(result.toDomain()).getOrThrow()
                    }
                }
            )

            dialogController.show(dialog)
        }
    }

    override fun onOpenTrainingProfile() {
        val range = state.value.range.value ?: return

        val dialog = DialogConfig.TrainingProfileDetails(
            range = range
        )

        dialogController.show(dialog)
    }

    override fun onOpenExample(id: String) {
        val dialog = DialogConfig.ExerciseExample(
            id = id
        )

        dialogController.show(dialog)
    }

    override fun onResumeTraining() {
        val config = DialogConfig.DraftTraining(
            onContinue = { navigateTo(HomeDirection.DraftTraining) },
            onStartNew = { navigateTo(HomeDirection.StartTraining(TrainingSeed.Blank)) }
        )

        dialogController.show(config)
    }

    override fun onOpenTrainingStreak() {
        val range = state.value.range.value ?: return

        val dialog = DialogConfig.TrainingStreakDetails(
            range = range
        )

        dialogController.show(dialog)
    }

    override fun onOpenGoalDetails() {
        val range = state.value.range.value ?: return

        val dialog = DialogConfig.TrainingGoalDetails(
            range = range,
            onAddGoal = { navigateTo(HomeDirection.Goal) }
        )

        dialogController.show(dialog)
    }

    override fun onOpenDigest() {
        val range = state.value.range.value ?: return

        val config = DialogConfig.Statistics.Trainings(
            range = range
        )

        dialogController.show(config)
    }

    override fun onAddGoal() {
        navigateTo(HomeDirection.Goal)
    }

    override fun onBack() {
        navigateTo(HomeDirection.Back)
    }

    private fun clearHome() {
        update {
            it.copy(
                totalDuration = null,
                spotlights = persistentListOf(),
                muscleLoad = null,
                streak = null,
                performance = persistentListOf(),
                lastTraining = null,
                profile = null,
                goalProgress = null,
            )
        }
    }
}
