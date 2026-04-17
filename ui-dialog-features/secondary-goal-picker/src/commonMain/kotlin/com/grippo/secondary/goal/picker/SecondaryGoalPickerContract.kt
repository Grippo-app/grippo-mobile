package com.grippo.secondary.goal.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState

@Immutable
internal interface SecondaryGoalPickerContract {
    fun onSelectGoal(goal: GoalSecondaryGoalEnumState)
    fun onDismiss()

    @Immutable
    companion object Empty : SecondaryGoalPickerContract {
        override fun onSelectGoal(goal: GoalSecondaryGoalEnumState) {}
        override fun onDismiss() {}
    }
}
