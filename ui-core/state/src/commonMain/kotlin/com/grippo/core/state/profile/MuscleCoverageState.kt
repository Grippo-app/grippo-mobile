package com.grippo.core.state.profile

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class MuscleCoverageState(
    val color: Color,
    val percentage: Int
)