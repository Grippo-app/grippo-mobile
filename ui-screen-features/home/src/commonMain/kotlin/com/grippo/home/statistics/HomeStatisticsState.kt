package com.grippo.home.statistics

import androidx.compose.runtime.Immutable
import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSHeatmapData
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSRadarData
import com.grippo.design.components.chart.DSSparklineData
import com.grippo.state.datetime.PeriodState
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState

@Immutable
internal data class HomeStatisticsState(
    val volume: VolumeFormatState = VolumeFormatState.of(""),
    val repetitions: RepetitionsFormatState = RepetitionsFormatState.of(""),
    val intensity: IntensityFormatState = IntensityFormatState.of(""),
    val period: PeriodState = PeriodState.ThisDay,
    val areaData: DSAreaData = DSAreaData(points = emptyList()),
    val barData: DSBarData = DSBarData(items = emptyList()),
    val heatmapData: DSHeatmapData = DSHeatmapData(rows = 1, cols = 1, values01 = listOf(0f)),
    val radarData: DSRadarData = DSRadarData(axes = emptyList(), series = emptyList()),
    val progressData: DSProgressData = DSProgressData(items = emptyList()),
    val sparklineData: DSSparklineData = DSSparklineData(points = emptyList()),
)