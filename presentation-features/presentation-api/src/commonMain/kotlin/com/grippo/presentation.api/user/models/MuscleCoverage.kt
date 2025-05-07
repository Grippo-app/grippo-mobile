package com.grippo.presentation.api.user.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class MuscleCoverage(
    val color: Color,
    val percentage: Int
)