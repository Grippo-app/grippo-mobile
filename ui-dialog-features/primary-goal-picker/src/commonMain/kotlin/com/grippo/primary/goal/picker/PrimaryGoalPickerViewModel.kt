package com.grippo.primary.goal.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState

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
        navigateTo(PrimaryGoalPickerDirection.BackWithResult(goal))
    }

    override fun onDismiss() {
        navigateTo(PrimaryGoalPickerDirection.Back)
    }
}
