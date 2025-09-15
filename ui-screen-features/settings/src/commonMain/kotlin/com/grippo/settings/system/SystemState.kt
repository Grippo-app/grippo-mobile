package com.grippo.settings.system

import androidx.compose.runtime.Immutable
import com.grippo.state.settings.LocaleState
import com.grippo.state.settings.ThemeState

@Immutable
internal data class SystemState(
    val theme: ThemeState? = null,
    val locale: LocaleState? = null
)