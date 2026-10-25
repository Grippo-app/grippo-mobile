package com.grippo.chart.radar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class RadarAxis(
    val id: String,
    val label: String
)

public data class RadarSeries(
    val name: String,
    val color: Color,
    val values: RadarValues,
)

public data class RadarData(
    val axes: List<RadarAxis>,
    val series: List<RadarSeries>,
)

public sealed interface RadarValues {
    public data class ByAxisId(val map: Map<String, Float>) :
        RadarValues

    public data class ByIndex(val list: List<Float>) :
        RadarValues
}
