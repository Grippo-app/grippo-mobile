package com.grippo.authorization.registration.completed

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.profile.models.UserState

@Immutable
internal data class CompletedState(
    val user: UserState? = null
)