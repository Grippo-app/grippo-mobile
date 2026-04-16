package com.grippo.profile.goal

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.goal.models.SetGoal
import com.grippo.domain.state.user.toState
import com.grippo.state.domain.goal.toDomain
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDate

internal class ProfileGoalViewModel(
    private val goalFeature: GoalFeature
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
        goal ?: return
        val goalState = goal.toState()
        update {
            it.copy(
                selectedPrimary = goalState.primaryGoal,
                selectedSecondary = goalState.secondaryGoal,
                selectedTarget = goalState.target,
                selectedPersonalization = goalState.personalizations.toPersistentSet(),
            )
        }
    }

    override fun onSelectPrimary(goal: GoalPrimaryGoalEnumState) {
        update { it.copy(selectedPrimary = goal) }
    }

    override fun onSaveFastPath() {

    }

    override fun onToggleCustomize() {
    }

    override fun onSelectSecondary(goal: GoalSecondaryGoalEnumState?) {
        update { current ->
            val next = if (current.selectedSecondary == goal) null else goal
            current.copy(selectedSecondary = next)
        }
    }

    override fun onSelectTargetDate(date: LocalDate?) {

    }

    override fun onToggleConstraint(constraint: PersonalizationKeyEnumState) {
        update { current ->
            val next = if (current.selectedPersonalization.contains(constraint)) {
                (current.selectedPersonalization - constraint).toPersistentSet()
            } else {
                (current.selectedPersonalization + constraint).toPersistentSet()
            }
            current.copy(selectedPersonalization = next)
        }
    }

    override fun onSave() {
        safeLaunch(loader = ProfileGoalLoader.SaveButton) {
            val request = SetGoal(
                primaryGoal = state.value.selectedPrimary?.toDomain() ?: return@safeLaunch,
                secondaryGoal = state.value.selectedSecondary?.toDomain(),
                target = state.value.selectedTarget ?: return@safeLaunch,
                personalizations = state.value.selectedPersonalization.map { it.toDomain() },
            )
            goalFeature.setGoal(request).getOrThrow()
            navigateTo(ProfileGoalDirection.Back)
        }
    }

    override fun onBack() {
        navigateTo(ProfileGoalDirection.Back)
    }
}
