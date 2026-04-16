package com.grippo.core.state.profile

import androidx.compose.runtime.Immutable
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

@Immutable
public data class GoalState(
    val primaryGoal: GoalPrimaryGoalEnumState,
    val secondaryGoal: GoalSecondaryGoalEnumState?,
    val target: LocalDateTime,
    val personalizations: ImmutableList<PersonalizationKeyEnumState>,
)

public fun stubGoal(): GoalState {
    return GoalState(
        primaryGoal = GoalPrimaryGoalEnumState.entries.random(),
        secondaryGoal = GoalSecondaryGoalEnumState.entries.random(),
        target = DateTimeUtils.now(),
        personalizations = PersonalizationKeyEnumState.entries.shuffled().take(3).toPersistentList()
    )
}