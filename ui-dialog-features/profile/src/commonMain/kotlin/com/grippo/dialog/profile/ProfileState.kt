package com.grippo.dialog.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.UserState

@Immutable
public data class ProfileState(
    val user: UserState? = null
)