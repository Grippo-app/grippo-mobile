package com.grippo.chart.ring

import androidx.compose.runtime.Immutable

@Immutable
public data class RingData(
    val value: Float,
    val min: Float = 0f,
    val max: Float = 100f,
)
