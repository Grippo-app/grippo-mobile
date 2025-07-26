package com.grippo.settings.system

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.settings.models.ThemeState

@Immutable
internal data class SystemState(
    val theme: ThemeState? = null
)