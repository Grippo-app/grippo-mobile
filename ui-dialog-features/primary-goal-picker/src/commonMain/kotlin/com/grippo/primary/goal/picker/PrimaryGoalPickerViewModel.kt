package com.grippo.primary.goal.picker

import com.grippo.analytics.primary.goal.picker.PrimaryGoalPickerAnalytics
import com.grippo.analytics.primary.goal.picker.models.PrimaryGoalPickerEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import kotlinx.coroutines.delay

public class PrimaryGoalPickerViewModel(
    initial: GoalPrimaryGoalEnumState?,
    private val analytics: PrimaryGoalPickerAnalytics,
) : BaseViewModel<PrimaryGoalPickerState, PrimaryGoalPickerDirection, PrimaryGoalPickerLoader>(
    PrimaryGoalPickerState(
        value = initial,
    )
), PrimaryGoalPickerContract {

    override fun onSelectGoal(goal: GoalPrimaryGoalEnumState) {
        safeLaunch {
            analytics.trackEvent(PrimaryGoalPickerEvent.GoalSelected)
            update { it.copy(value = goal) }
            delay(300)
            navigateTo(PrimaryGoalPickerDirection.BackWithResult(goal))
        }
    }

    override fun onDismiss() {
        analytics.trackEvent(PrimaryGoalPickerEvent.DismissRequested)
        navigateTo(PrimaryGoalPickerDirection.Back)
    }
}
