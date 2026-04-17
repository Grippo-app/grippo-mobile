package com.grippo.primary.goal.picker

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState

public sealed interface PrimaryGoalPickerDirection : BaseDirection {
    public data class BackWithResult(val value: GoalPrimaryGoalEnumState) :
        PrimaryGoalPickerDirection

    public data object Back : PrimaryGoalPickerDirection
}
