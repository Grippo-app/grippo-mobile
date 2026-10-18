package com.grippo.profile.settings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.UserState

@Immutable
internal data class ProfileSettingsState(
    val analyticsConsent: Boolean? = null,
    val user: UserState? = null,
)
