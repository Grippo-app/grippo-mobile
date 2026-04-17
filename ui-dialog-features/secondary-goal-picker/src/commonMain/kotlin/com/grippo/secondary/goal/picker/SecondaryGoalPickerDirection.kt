package com.grippo.secondary.goal.picker

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState

public sealed interface SecondaryGoalPickerDirection : BaseDirection {
    public data class BackWithResult(val value: GoalSecondaryGoalEnumState) :
        SecondaryGoalPickerDirection

    public data object Back : SecondaryGoalPickerDirection
}
