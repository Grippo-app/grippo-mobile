package com.grippo.primary.goal.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import kotlinx.coroutines.delay

public class PrimaryGoalPickerViewModel(
    title: String,
    initial: GoalPrimaryGoalEnumState?,
) : BaseViewModel<PrimaryGoalPickerState, PrimaryGoalPickerDirection, PrimaryGoalPickerLoader>(
    PrimaryGoalPickerState(
        value = initial,
        title = title,
    )
), PrimaryGoalPickerContract {

    override fun onSelectGoal(goal: GoalPrimaryGoalEnumState) {
        safeLaunch {
            update { it.copy(value = goal) }
            delay(200)
            navigateTo(PrimaryGoalPickerDirection.BackWithResult(goal))
        }
    }

    override fun onDismiss() {
        navigateTo(PrimaryGoalPickerDirection.Back)
    }
}
