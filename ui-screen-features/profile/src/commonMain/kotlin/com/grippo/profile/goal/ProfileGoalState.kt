package com.grippo.profile.goal

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.DatePeriod

@Immutable
internal data class ProfileGoalState(
    val selectedPrimary: GoalPrimaryGoalEnumState? = null,
    val selectedSecondary: GoalSecondaryGoalEnumState? = null,

    val limitations: DateRange = DateTimeUtils.shift(
        DateTimeUtils.leadingYear(),
        DatePeriod(days = MIN_GOAL_HORIZON_DAYS)
    ),
    val selectedTarget: DateTimeFormatState = DateTimeFormatState.of(
        value = limitations.from,
        range = limitations,
        format = DateFormat.DateOnly.DateMmmDdYyyy
    ),

    val selectedPersonalization: ImmutableSet<PersonalizationKeyEnumState> = persistentSetOf(),

    val primaryGoals: ImmutableList<GoalPrimaryGoalEnumState> = GoalPrimaryGoalEnumState.entries.toPersistentList(),
    val secondaryGoals: ImmutableList<GoalSecondaryGoalEnumState> = GoalSecondaryGoalEnumState.entries.toPersistentList(),
    val personalization: ImmutableList<PersonalizationKeyEnumState> = PersonalizationKeyEnumState.entries.toPersistentList()
) {
    internal companion object {
        // Minimum horizon for a training goal — below 4 weeks no meaningful adaptation occurs
        // (mesocycle floor: strength neural adaptations ~2-4w, hypertrophy ~4-6w, safe fat-loss pace).
        const val MIN_GOAL_HORIZON_DAYS: Int = 28
    }
}
