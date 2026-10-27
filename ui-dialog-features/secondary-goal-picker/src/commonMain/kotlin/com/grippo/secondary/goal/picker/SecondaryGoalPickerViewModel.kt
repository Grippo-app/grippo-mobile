package com.grippo.secondary.goal.picker

import com.grippo.analytics.secondary.goal.picker.SecondaryGoalPickerAnalytics
import com.grippo.analytics.secondary.goal.picker.models.SecondaryGoalPickerEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

public class SecondaryGoalPickerViewModel(
    private val analytics: SecondaryGoalPickerAnalytics,
    initial: GoalSecondaryGoalEnumState?,
) : BaseViewModel<SecondaryGoalPickerState, SecondaryGoalPickerDirection, SecondaryGoalPickerLoader>(
    SecondaryGoalPickerState(
        value = initial,
    )
), SecondaryGoalPickerContract {

    override fun onSelectGoal(goal: GoalSecondaryGoalEnumState) {
        analytics.trackEvent(SecondaryGoalPickerEvent.GoalClicked)

        safeLaunch(recovery = ErrorRecovery.Retry) {
            update { it.copy(value = goal) }
            delay(300.milliseconds)
            navigateTo(SecondaryGoalPickerDirection.BackWithResult(goal))
        }
    }

    override fun onDismiss() {
        analytics.trackEvent(SecondaryGoalPickerEvent.DismissRequested)
        navigateTo(SecondaryGoalPickerDirection.Back)
    }
}
