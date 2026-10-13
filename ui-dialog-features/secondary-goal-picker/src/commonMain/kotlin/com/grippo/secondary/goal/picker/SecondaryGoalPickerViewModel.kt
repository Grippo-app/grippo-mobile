package com.grippo.secondary.goal.picker

import com.grippo.analytics.secondary.goal.picker.SecondaryGoalPickerAnalytics
import com.grippo.analytics.secondary.goal.picker.models.SecondaryGoalPickerEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import kotlinx.coroutines.delay

public class SecondaryGoalPickerViewModel(
    private val analytics: SecondaryGoalPickerAnalytics,
    initial: GoalSecondaryGoalEnumState?,
) : BaseViewModel<SecondaryGoalPickerState, SecondaryGoalPickerDirection, SecondaryGoalPickerLoader>(
    SecondaryGoalPickerState(
        value = initial,
    )
), SecondaryGoalPickerContract {

    override fun onSelectGoal(goal: GoalSecondaryGoalEnumState) {
        safeLaunch {
            analytics.trackEvent(SecondaryGoalPickerEvent.GoalClicked)
            update { it.copy(value = goal) }
            delay(300)
            navigateTo(SecondaryGoalPickerDirection.BackWithResult(goal))
        }
    }

    override fun onDismiss() {
        analytics.trackEvent(SecondaryGoalPickerEvent.DismissRequested)
        navigateTo(SecondaryGoalPickerDirection.Back)
    }
}
