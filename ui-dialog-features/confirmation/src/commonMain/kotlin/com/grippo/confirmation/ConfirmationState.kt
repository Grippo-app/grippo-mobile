package com.grippo.confirmation

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText

@Immutable
public data class ConfirmationState(
    val description: UiText?,
)
