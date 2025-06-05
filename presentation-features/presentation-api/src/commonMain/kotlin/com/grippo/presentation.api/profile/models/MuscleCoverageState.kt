package com.grippo.presentation.api.profile.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class MuscleCoverageState(
    val color: Color,
    val percentage: Int
)