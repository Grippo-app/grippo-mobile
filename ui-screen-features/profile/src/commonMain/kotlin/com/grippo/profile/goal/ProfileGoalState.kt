package com.grippo.profile.goal

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.profile.PersonalizationKeyEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

@Immutable
internal data class ProfileGoalState(
    val selectedPrimary: GoalPrimaryGoalEnumState? = null,
    val selectedSecondary: GoalSecondaryGoalEnumState? = null,
    val selectedTarget: LocalDateTime? = null,
    val selectedPersonalization: ImmutableSet<PersonalizationKeyEnumState> = persistentSetOf(),

    val primaryGoals: ImmutableList<GoalPrimaryGoalEnumState> = GoalPrimaryGoalEnumState.entries.toPersistentList(),
    val secondaryGoals: ImmutableList<GoalSecondaryGoalEnumState> = GoalSecondaryGoalEnumState.entries.toPersistentList(),
    val personalization: ImmutableList<PersonalizationKeyEnumState> = PersonalizationKeyEnumState.entries.toPersistentList()
)
