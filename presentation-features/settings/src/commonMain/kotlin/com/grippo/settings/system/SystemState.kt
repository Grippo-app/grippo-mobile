package com.grippo.settings.system

import androidx.compose.runtime.Immutable
import com.grippo.state.settings.ThemeState

@Immutable
internal data class SystemState(
    val theme: ThemeState? = null
)