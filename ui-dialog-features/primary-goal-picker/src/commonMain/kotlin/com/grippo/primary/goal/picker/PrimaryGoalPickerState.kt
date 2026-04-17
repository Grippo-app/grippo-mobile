package com.grippo.primary.goal.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class PrimaryGoalPickerState(
    val title: String,
    val value: GoalPrimaryGoalEnumState?,
    val goals: ImmutableList<GoalPrimaryGoalEnumState> = GoalPrimaryGoalEnumState.entries.toPersistentList(),
)
