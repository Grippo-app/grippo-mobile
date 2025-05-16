package com.grippo.authorization.registration.completed

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.user.models.UserState

@Immutable
internal data class CompletedState(
    val user: UserState? = null
)