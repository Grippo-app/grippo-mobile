package com.grippo.profile.goal

import com.grippo.analytics.profile.ProfileAnalytics
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.core.foundation.BaseViewModel
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
                .safeLaunch(processing = Processing.Infinity)
        }

        safeLaunch {
            goalFeature.getGoal().getOrThrow()
        }
    }

    private fun provideGoal(goal: Goal) {
        val goalState = goal.toState()
        update {
            it.copy(
                selectedPrimary = it.selectedPrimary ?: goalState.primaryGoal,
                selectedSecondary = it.selectedSecondary ?: goalState.secondaryGoal,
                selectedTarget = if (it.selectedTarget.value == it.limitations.from) {
                    goalState.target
                } else {
                    it.selectedTarget
                },
                selectedPersonalization = if (it.selectedPersonalization.isEmpty()) {
                    goalState.personalizations.distinct().toPersistentList()
                } else {
                    it.selectedPersonalization
                },
            )
        }
    }

    override fun onSave() {
        safeLaunch(loader = ProfileGoalLoader.SaveButton) {
            val request = SetGoal(
                primaryGoal = state.value.selectedPrimary?.toDomain() ?: return@safeLaunch,
                secondaryGoal = state.value.selectedSecondary?.toDomain(),
                target = state.value.selectedTarget.value ?: return@safeLaunch,
                personalizations = state.value.selectedPersonalization.map { it.toDomain() },
            )
            goalFeature.setGoal(request).getOrThrow()
            navigateTo(ProfileGoalDirection.Back)
        }
    }

    override fun onTargetDatePickerClick() {
        val config = DialogConfig.DatePicker(
            initial = state.value.selectedTarget.value,
            format = state.value.selectedTarget.format,
            limitations = state.value.limitations,
            title = UiText.Res(Res.string.select_date),
            onResult = { result ->
                update {
                    it.copy(
                        selectedTarget = DateTimeFormatState.of(
                            value = result,
                            range = it.limitations,
                            format = it.selectedTarget.format,
                        ),
                    )
                }
            }
        )

        dialogController.open(config)
    }

    override fun onPrimaryGoalPickerClick() {
        val config = DialogConfig.PrimaryGoalPicker(
            initial = state.value.selectedPrimary,
            title = UiText.Res(Res.string.goal_picker_primary_title),
            onResult = { value -> update { it.copy(selectedPrimary = value) } }
        )

        dialogController.open(config)
    }

    override fun onSecondaryGoalPickerClick() {
        val config = DialogConfig.SecondaryGoalPicker(
            initial = state.value.selectedSecondary,
            title = UiText.Res(Res.string.goal_picker_secondary_title),
            onResult = { value -> update { it.copy(selectedSecondary = value) } }
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
    }

    override fun onBack() {
        navigateTo(ProfileGoalDirection.Back)
    }
}
