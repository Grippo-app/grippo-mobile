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
        range = DateTimeUtils.leadingYear(),
        period = DatePeriod(days = MIN_GOAL_HORIZON_DAYS),
    ),
    val selectedTarget: DateTimeFormatState = DateTimeFormatState.Empty(
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    ),
    @Serializable(with = ImmutableListSerializer::class)
    val selectedPersonalization: ImmutableList<PersonalizationKeyEnumState> = persistentListOf(),
    @Transient
    val personalization: ImmutableList<PersonalizationKeyEnumState> =
        PersonalizationKeyEnumState.entries.toPersistentList(),
) {
    val targetDate: DateTimeFormatState
        get() {
            // An ongoing goal can be closer than the minimum horizon for a new target.
            val currentPeriod = DateTimeUtils.shift(
                range = limitations,
                period = DatePeriod(days = -MIN_GOAL_HORIZON_DAYS),
            )
            return DateTimeFormatState.of(
                value = selectedTarget.value,
                range = DateRange(from = currentPeriod.from, to = limitations.to),
                format = selectedTarget.format,
            )
        }

    internal companion object {
        const val MIN_GOAL_HORIZON_DAYS: Int = 28

        fun initial(): ProfileGoalState {
            val state = ProfileGoalState()
            val target = DateTimeFormatState.of(
                value = state.limitations.from,
                range = state.limitations,
                format = state.selectedTarget.format,
            )

            return state.copy(selectedTarget = target)
        }
    }
}
