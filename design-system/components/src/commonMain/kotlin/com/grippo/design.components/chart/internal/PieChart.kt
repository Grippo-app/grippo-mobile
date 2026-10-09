package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.chart.pie.PieChart
import com.grippo.chart.pie.PieData
import com.grippo.chart.pie.PieSlice
import com.grippo.chart.pie.PieStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview

@Composable
internal fun PieChart(
    modifier: Modifier = Modifier,
    data: PieData
) {
    val style = PieStyle(
        layout = PieStyle.Layout(
            padding = AppTokens.dp.metrics.charts.pie.layoutPadding,
            startAngleDeg = -60f,
        ),
        arc = PieStyle.Arc(
            width = AppTokens.dp.metrics.charts.pie.arcWidth,
            paddingAngleDeg = 2f,
            cornerRadius = AppTokens.dp.metrics.charts.pie.radius,
            minVisualPercent = 0.15f,   // ← 15% minimum per slice (visual)
        ),
        labels = PieStyle.Labels.Outside(
            textStyle = AppTokens.typography.b10Bold().copy(color = AppTokens.colors.text.primary),
            labelPadding = AppTokens.dp.metrics.charts.pie.labelPadding,
            formatter = { slice, _ -> slice.label }
        ),
        leaders = PieStyle.Leaders(
            show = true,
            lineWidth = AppTokens.dp.metrics.charts.pie.leaderWidth,
            offset = AppTokens.dp.metrics.charts.pie.leaderOffset,
        )
    )

    PieChart(
        modifier = modifier,
        data = data,
        style = style
    )
}

@AppPreview
@Composable
private fun PieChartPreview() {
    val data = PieData(
        slices = listOf(
            PieSlice("legs", "Legs", 26f, AppTokens.colors.muscle.palette6MuscleCalm[0]),
            PieSlice("back", "Back", 18f, AppTokens.colors.muscle.palette6MuscleCalm[1]),
            PieSlice("chest", "Chest", 22f, AppTokens.colors.muscle.palette6MuscleCalm[2]),
            PieSlice("arms", "Arms", 12f, AppTokens.colors.muscle.palette6MuscleCalm[3]),
            PieSlice("shoulders", "Shoulders", 10f, AppTokens.colors.muscle.palette6MuscleCalm[4]),
            PieSlice("core", "Core", 12f, AppTokens.colors.muscle.palette6MuscleCalm[5]),
        )
    )

    PieChart(
        modifier = Modifier.size(300.dp),
        data = data,
    )
}
