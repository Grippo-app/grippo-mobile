package com.grippo.wheel.picker

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
public data class WheelConfig(
    val rowCount: Int,
    val itemHeight: Dp,
)