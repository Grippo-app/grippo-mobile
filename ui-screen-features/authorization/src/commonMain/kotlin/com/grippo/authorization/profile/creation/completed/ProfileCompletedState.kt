package com.grippo.authorization.profile.creation.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.UserState

@Immutable
internal data class ProfileCompletedState(
    val user: UserState? = null
)