package com.grippo.authorization.registration.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.UserState

@Immutable
internal data class CompletedState(
    val user: UserState? = null
)