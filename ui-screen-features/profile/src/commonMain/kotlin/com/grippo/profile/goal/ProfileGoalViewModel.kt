package com.grippo.profile.goal

import com.grippo.analytics.profile.ProfileAnalytics
import com.grippo.analytics.profile.models.GoalPicker
import com.grippo.analytics.profile.models.ProfileEvent
import com.grippo.analytics.profile.models.ProfileOperation
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.goal.models.SetGoal
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_picker_primary_title
import com.grippo.design.resources.provider.goal_picker_secondary_title
import com.grippo.design.resources.provider.select_date
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.user.toState
import com.grippo.profile.analytics.ProfileInputMapping
import com.grippo.state.domain.goal.toDomain
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take

internal class ProfileGoalViewModel(
    private val analytics: ProfileAnalytics,
    private val goalFeature: GoalFeature,
    private val dialogController: DialogController,
    initialState: ProfileGoalState?,
) : BaseViewModel<ProfileGoalState, ProfileGoalDirection, ProfileGoalLoader>(
    initialState ?: ProfileGoalState.initial()
), ProfileGoalContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(ProfileScreen.Goal, reason)
    }

    init {
        if (initialState == null) {
            goalFeature.observeGoal()
                .filterNotNull()
                .take(1)
                .onEach(::provideGoal)
                .safeLaunch(processing = Processing.Infinity, recovery = ErrorRecovery.Retry)
        }

        safeLaunch(recovery = ErrorRecovery.Retry) {
            analytics.trackOperation(ProfileOperation.RefreshGoal) {
                goalFeature.getGoal()
            }
        }
    }

    private fun provideGoal(goal: Goal) {
        val goalState = goal.toState()

        val goalPersonalizations = goalState.personalizations.distinct().toPersistentList()

        update { current ->
            val primary = current.selectedPrimary ?: goalState.primaryGoal
            val secondary = current.selectedSecondary ?: goalState.secondaryGoal
            val target = if (current.selectedTarget.value == current.limitations.from) {
                goalState.target
            } else {
                current.selectedTarget
            }
            val personalizations = if (current.selectedPersonalization.isEmpty()) {
                goalPersonalizations
            } else {
                current.selectedPersonalization
            }

            current.copy(
                selectedPrimary = primary,
                selectedSecondary = secondary,
                selectedTarget = target,
                selectedPersonalization = personalizations,
            )
        }
    }

    override fun onSave() {
        val current = state.value
        val targetDate = current.targetDate as? DateTimeFormatState.Valid ?: return

        val primaryGoal = current.selectedPrimary?.toDomain() ?: return

        val secondaryGoal = current.selectedSecondary?.toDomain()

        val personalizations = current.selectedPersonalization.map { it.toDomain() }

        val request = SetGoal(
            primaryGoal = primaryGoal,
            secondaryGoal = secondaryGoal,
            target = targetDate.value,
            personalizations = personalizations,
        )

        analytics.trackEvent(ProfileEvent.Goal.Save)

        safeLaunch(loader = ProfileGoalLoader.SaveButton, recovery = ErrorRecovery.Retry) {
            analytics.trackOperation(ProfileOperation.SaveGoal) {
                goalFeature.setGoal(request)
            }

            navigateTo(ProfileGoalDirection.Back)
        }
    }

    override fun onTargetDatePickerClick() {
        analytics.trackEvent(ProfileEvent.Goal.PickerOpened(field = GoalPicker.TargetDate))

        val config = DialogConfig.DatePicker(
            initial = state.value.selectedTarget.value,
            format = state.value.selectedTarget.format,
            limitations = state.value.limitations,
            title = UiText.Res(Res.string.select_date),
            onResult = { result ->
                val current = state.value
                val target = DateTimeFormatState.of(
                    value = result,
                    range = current.limitations,
                    format = current.selectedTarget.format,
                )

                update { it.copy(selectedTarget = target) }

                val status = ProfileInputMapping.status(state.value.selectedTarget)

                analytics.trackEvent(
                    ProfileEvent.Goal.TargetDateConfirmed(status = status)
                )
            },
        )

        dialogController.open(config)
    }

    override fun onPrimaryGoalPickerClick() {
        analytics.trackEvent(ProfileEvent.Goal.PickerOpened(field = GoalPicker.Primary))

        val config = DialogConfig.PrimaryGoalPicker(
            initial = state.value.selectedPrimary,
            title = UiText.Res(Res.string.goal_picker_primary_title),
            onResult = { value ->
                update { it.copy(selectedPrimary = value) }
                analytics.trackEvent(ProfileEvent.Goal.PrimaryConfirmed)
            },
        )

        dialogController.open(config)
    }

    override fun onSecondaryGoalPickerClick() {
        analytics.trackEvent(ProfileEvent.Goal.PickerOpened(field = GoalPicker.Secondary))

        val config = DialogConfig.SecondaryGoalPicker(
            initial = state.value.selectedSecondary,
            title = UiText.Res(Res.string.goal_picker_secondary_title),
            onResult = { value ->
                update { it.copy(selectedSecondary = value) }
                analytics.trackEvent(ProfileEvent.Goal.SecondaryConfirmed)
            },
        )

        dialogController.open(config)
    }

    override fun onPersonalizationClick(item: PersonalizationKeyEnumState) {
        update { current ->
            val selected = current.selectedPersonalization.toPersistentList()
            val personalizations = if (item in selected) {
                selected.removing(item)
            } else {
                selected.adding(item)
            }

            current.copy(selectedPersonalization = personalizations)
        }

        val selected = state.value.selectedPersonalization

        analytics.trackEvent(
            ProfileEvent.Goal.PersonalizationChanged(
                selected = item in selected,
                selectedCount = selected.size,
            )
        )
    }

    override fun onBack() {
        analytics.trackEvent(ProfileEvent.Goal.DismissRequested)
        navigateTo(ProfileGoalDirection.Back)
    }
}
