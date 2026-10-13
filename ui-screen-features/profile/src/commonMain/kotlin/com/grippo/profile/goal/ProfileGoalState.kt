package com.grippo.profile.goal

import androidx.compose.runtime.Immutable
import com.grippo.core.state.ImmutableListSerializer
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.DatePeriod
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Immutable
@Serializable
@JsonIgnoreUnknownKeys
internal data class ProfileGoalState(
    val selectedPrimary: GoalPrimaryGoalEnumState? = null,
    val selectedSecondary: GoalSecondaryGoalEnumState? = null,

    @Transient
    val limitations: DateRange = DateTimeUtils.shift(
        DateTimeUtils.leadingYear(),
        DatePeriod(days = MIN_GOAL_HORIZON_DAYS)
    ),
    val selectedTarget: DateTimeFormatState = DateTimeFormatState.Empty(
        format = DateFormat.DateOnly.DateMmmDdYyyy
    ),

    @Serializable(with = ImmutableListSerializer::class)
    val selectedPersonalization: ImmutableList<PersonalizationKeyEnumState> = persistentListOf(),

    @Transient
    val personalization: ImmutableList<PersonalizationKeyEnumState> = PersonalizationKeyEnumState.entries.toPersistentList()
) {
    internal companion object {
        // Minimum horizon for a training goal — below 4 weeks no meaningful adaptation occurs
        // (mesocycle floor: strength neural adaptations ~2-4w, hypertrophy ~4-6w, safe fat-loss pace).
        const val MIN_GOAL_HORIZON_DAYS: Int = 28

        fun initial(): ProfileGoalState {
            val state = ProfileGoalState()
            return state.copy(
                selectedTarget = DateTimeFormatState.of(
                    value = state.limitations.from,
                    range = state.limitations,
                    format = state.selectedTarget.format,
                )
            )
        }
    }
}
