package com.grippo.secondary.goal.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState

public class SecondaryGoalPickerViewModel(
    title: String,
    initial: GoalSecondaryGoalEnumState?,
) : BaseViewModel<SecondaryGoalPickerState, SecondaryGoalPickerDirection, SecondaryGoalPickerLoader>(
    SecondaryGoalPickerState(
        value = initial,
        title = title,
    )
), SecondaryGoalPickerContract {

    override fun onSelectGoal(goal: GoalSecondaryGoalEnumState) {
        navigateTo(SecondaryGoalPickerDirection.BackWithResult(goal))
    }

    override fun onDismiss() {
        navigateTo(SecondaryGoalPickerDirection.Back)
    }
}
