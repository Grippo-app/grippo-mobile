package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            padding = 0.dp,
            startAngleDeg = -60f,
        ),
        arc = PieStyle.Arc(
            width = 24.dp,
            paddingAngleDeg = 2f,
            cornerRadius = 6.dp,
            minVisualPercent = 0.15f,   // ← 15% minimum per slice (visual)
        ),
        labels = PieStyle.Labels.Adaptive(
            insideMinAngleDeg = 14f,
            outsideMinAngleDeg = 5f,
            textStyle = AppTokens.typography.b10Bold().copy(color = AppTokens.colors.text.primary),
            labelPadding = 6.dp,
            formatter = { s, p -> s.label }
        ),
        leaders = PieStyle.Leaders(
            show = true,
            lineWidth = 1.dp,
            offset = 8.dp,
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
            PieSlice("legs", "Legs", 26f, Color(0xFFFF7A33)),
            PieSlice("back", "Back", 18f, Color(0xFF6AA9FF)),
            PieSlice("chest", "Chest", 22f, Color(0xFF00E6A7)),
            PieSlice("arms", "Arms", 12f, Color(0xFFFFC53D)),
            PieSlice("shoulders", "Shoulders", 10f, Color(0xFFB049F8)),
            PieSlice("core", "Core", 12f, Color(0xFFFF5E8A)),
        )
    )

    PieChart(
        modifier = Modifier.size(300.dp),
        data = data,
    )
}
