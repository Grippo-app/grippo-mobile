package com.grippo.core.state.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class GoalState(
    val primaryGoal: GoalPrimaryGoalEnumState,
    val secondaryGoal: GoalSecondaryGoalEnumState?,
    val target: DateFormatState,
    val createdAt: DateFormatState,
    val personalizations: ImmutableList<PersonalizationKeyEnumState>,
)

public fun stubGoal(): GoalState {
    return GoalState(
        primaryGoal = GoalPrimaryGoalEnumState.entries.random(),
        secondaryGoal = GoalSecondaryGoalEnumState.entries.random(),
        target = DateFormatState.of(
            value = DateTimeUtils.now(),
            range = DateTimeUtils.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy
        ),
        createdAt = DateFormatState.of(
            value = DateTimeUtils.now(),
            range = DateTimeUtils.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy
        ),
        personalizations = PersonalizationKeyEnumState.entries.shuffled().take(3).toPersistentList()
    )
}
