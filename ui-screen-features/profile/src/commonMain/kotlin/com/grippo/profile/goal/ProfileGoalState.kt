package com.grippo.profile.goal

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Immutable
internal data class ProfileGoalState(
    val selectedPrimary: GoalPrimaryGoalEnumState? = null,
    val selectedSecondary: GoalSecondaryGoalEnumState? = null,

    val limitations: DateRange = DateTimeUtils.leadingYear(),
    val selectedTarget: DateFormatState = DateFormatState.of(
        value = DateTimeUtils.now(),
        range = limitations
    ),

    val selectedPersonalization: ImmutableSet<PersonalizationKeyEnumState> = persistentSetOf(),

    val primaryGoals: ImmutableList<GoalPrimaryGoalEnumState> = GoalPrimaryGoalEnumState.entries.toPersistentList(),
    val secondaryGoals: ImmutableList<GoalSecondaryGoalEnumState> = GoalSecondaryGoalEnumState.entries.toPersistentList(),
    val personalization: ImmutableList<PersonalizationKeyEnumState> = PersonalizationKeyEnumState.entries.toPersistentList()
)
