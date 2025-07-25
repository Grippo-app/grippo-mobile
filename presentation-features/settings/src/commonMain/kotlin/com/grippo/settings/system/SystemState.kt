package com.grippo.settings.system

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.settings.models.ColorModeState

@Immutable
internal data class SystemState(
    val colorMode: ColorModeState? = null
)