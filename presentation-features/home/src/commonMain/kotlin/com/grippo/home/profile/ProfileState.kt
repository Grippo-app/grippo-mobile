package com.grippo.home.profile

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.user.models.UserState

@Immutable
internal data class ProfileState(
    val user: UserState? = null
)