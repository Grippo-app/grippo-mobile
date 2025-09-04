package com.grippo.calculation.models

import androidx.compose.runtime.Immutable
import com.grippo.state.formatters.UiText

@Immutable
public data class Instruction(
    val title: UiText,
    val description: UiText,
)