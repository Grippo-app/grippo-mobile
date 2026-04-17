package com.grippo.primary.goal.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState

@Immutable
internal interface PrimaryGoalPickerContract {
    fun onSelectGoal(goal: GoalPrimaryGoalEnumState)
    fun onDismiss()

    @Immutable
    companion object Empty : PrimaryGoalPickerContract {
        override fun onSelectGoal(goal: GoalPrimaryGoalEnumState) {}
        override fun onDismiss() {}
    }
}
