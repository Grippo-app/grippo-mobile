package com.grippo.authorization.profile.creation.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.core.state.profile.UserState

@Immutable
internal data class ProfileCompletedState(
    val user: UserState? = null,
    val experience: ExperienceEnumState? = null,
    val excludedMusclesCount: Int = 0,
    val missingEquipmentCount: Int = 0,
)
