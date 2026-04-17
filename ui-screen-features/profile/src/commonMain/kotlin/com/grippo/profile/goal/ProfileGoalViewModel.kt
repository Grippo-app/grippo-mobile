package com.grippo.profile.goal

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_picker_primary_title
import com.grippo.design.resources.provider.goal_picker_secondary_title
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.select_date
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.user.toState
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.onEach

internal class ProfileGoalViewModel(
    private val goalFeature: GoalFeature,
    private val dialogController: DialogController,
    private val stringProvider: StringProvider,
) : BaseViewModel<ProfileGoalState, ProfileGoalDirection, ProfileGoalLoader>(
    ProfileGoalState()
), ProfileGoalContract {

    init {
        goalFeature.observeGoal()
            .onEach(::provideGoal)
            .safeLaunch()

        safeLaunch {
            goalFeature.getGoal().getOrThrow()
        }
    }

    private fun provideGoal(goal: Goal?) {
        val goalState = goal?.toState() ?: return
        update {
            it.copy(
                selectedPrimary = goalState.primaryGoal,
                selectedSecondary = goalState.secondaryGoal,
                selectedTarget = goalState.target,
                selectedPersonalization = goalState.personalizations.toPersistentSet(),
            )
        }
    }

    override fun onSave() {
        safeLaunch(loader = ProfileGoalLoader.SaveButton) {
//            val request = SetGoal(
//                primaryGoal = state.value.selectedPrimary?.toDomain() ?: return@safeLaunch,
//                secondaryGoal = state.value.selectedSecondary?.toDomain(),
//                target = state.value.selectedTarget ?: return@safeLaunch,
//                personalizations = state.value.selectedPersonalization.map { it.toDomain() },
//            )
//            goalFeature.setGoal(request).getOrThrow()
//            navigateTo(ProfileGoalDirection.Back)
        }
    }

    override fun onTargetDatePickerClick() {
        safeLaunch {
            val config = DialogConfig.DatePicker(
                initial = state.value.selectedTarget,
                limitations = state.value.limitations,
                title = stringProvider.get(Res.string.select_date),
                onResult = {
                    update { current -> current.copy(selectedTarget = it) }
                }
            )

            dialogController.show(config)
        }
    }

    override fun onPrimaryGoalPickerClick() {
        safeLaunch {
            val config = DialogConfig.PrimaryGoalPicker(
                initial = state.value.selectedPrimary,
                title = stringProvider.get(Res.string.goal_picker_primary_title),
                onResult = { update { current -> current.copy(selectedPrimary = it) } }
            )

            dialogController.show(config)
        }
    }

    override fun onSecondaryGoalPickerClick() {
        safeLaunch {
            val config = DialogConfig.SecondaryGoalPicker(
                initial = state.value.selectedSecondary,
                title = stringProvider.get(Res.string.goal_picker_secondary_title),
                onResult = { update { current -> current.copy(selectedSecondary = it) } }
            )

            dialogController.show(config)
        }
    }

    override fun onBack() {
        navigateTo(ProfileGoalDirection.Back)
    }
}
