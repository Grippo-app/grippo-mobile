package com.grippo.chart.radar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class RadarAxis(
    val id: String,
    val label: String
)

@Immutable
public data class RadarSeries(
    val name: String,
    val color: Color,
    val values: RadarValues, // supports ByAxisId or ByIndex
)

@Immutable
public data class RadarData(
    val axes: List<RadarAxis>,
    val series: List<RadarSeries>,
)

@Immutable
public sealed interface RadarValues {
    @Immutable
    public data class ByAxisId(val map: Map<String, Float>) :
        RadarValues // key: axisId, value: 0..1

    @Immutable
    public data class ByIndex(val list: List<Float>) :
        RadarValues         // index aligned with axes
}
