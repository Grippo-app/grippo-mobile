package com.grippo.profile.menu

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.GoalState
import com.grippo.core.state.profile.UserState

@Immutable
internal data class ProfileMenuState(
    val user: UserState? = null,
    val goal: GoalState? = null,
    val excludedMusclesCount: Int? = null,
    val excludedEquipmentsCount: Int? = null,
)
