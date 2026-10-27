package com.grippo.primary.goal.picker

import com.grippo.analytics.primary.goal.picker.PrimaryGoalPickerAnalytics
import com.grippo.analytics.primary.goal.picker.models.PrimaryGoalPickerEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import kotlin.time.Duration.Companion.milliseconds
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
        analytics.trackEvent(PrimaryGoalPickerEvent.GoalClicked)

        safeLaunch(recovery = ErrorRecovery.Retry) {
            update { it.copy(value = goal) }
            delay(300.milliseconds)
            navigateTo(PrimaryGoalPickerDirection.BackWithResult(goal))
        }
    }

    override fun onDismiss() {
        analytics.trackEvent(PrimaryGoalPickerEvent.DismissRequested)
        navigateTo(PrimaryGoalPickerDirection.Back)
    }
}
