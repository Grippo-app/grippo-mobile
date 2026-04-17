package com.grippo.secondary.goal.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class SecondaryGoalPickerState(
    val title: String,
    val value: GoalSecondaryGoalEnumState?,
    val suggestions: ImmutableList<GoalSecondaryGoalEnumState> = GoalSecondaryGoalEnumState.entries.toPersistentList(),
)
