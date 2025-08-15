package com.grippo.home.statistics

import androidx.compose.runtime.Immutable
import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSHeatmapData
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSRadarData
import com.grippo.design.components.chart.DSSparklineData
import com.grippo.state.datetime.PeriodState

@Immutable
internal data class HomeStatisticsState(
    val period: PeriodState = PeriodState.ThisDay,
    val areaData: DSAreaData = DSAreaData(points = emptyList()),
    val barData: DSBarData = DSBarData(items = emptyList()),
    val heatmapData: DSHeatmapData = DSHeatmapData(rows = 1, cols = 1, values01 = listOf(0f)),
    val radarData: DSRadarData = DSRadarData(axes = emptyList(), series = emptyList()),
    val progressData: DSProgressData = DSProgressData(items = emptyList()),
    val sparklineData: DSSparklineData = DSSparklineData(points = emptyList()),
)