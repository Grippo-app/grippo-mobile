package com.grippo.design.components.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.grippo.chart.pie.PieChart
import com.grippo.chart.pie.PieData
import com.grippo.chart.pie.PieSlice
import com.grippo.chart.pie.PieStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview

@Composable
public fun PieChart(
    modifier: Modifier = Modifier,
    data: PieData
) {
    val style = PieStyle(
        layout = PieStyle.Layout(
            padding = 0.dp,
            startAngleDeg = -90f,
        ),
        arc = PieStyle.Arc(
            width = 28.dp,
            paddingAngleDeg = 2f,
            cornerRadius = 6.dp,
        ),
        labels = PieStyle.Labels(
            insideMinAngleDeg = 14f,
            outsideMinAngleDeg = 5f,
            textStyle = AppTokens.typography.b11Bold().copy(
                color = AppTokens.colors.muscle.text
            ),
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

    val style = PieStyle(
        layout = PieStyle.Layout(
            padding = 12.dp,
            startAngleDeg = -90f,
        ),
        arc = PieStyle.Arc(
            width = 28.dp,
            paddingAngleDeg = 2f,
            cornerRadius = 6.dp,
        ),
        labels = PieStyle.Labels(
            insideMinAngleDeg = 14f,
            outsideMinAngleDeg = 5f,
            textStyle = TextStyle(color = Color(0xCCFFFFFF)),
            labelPadding = 6.dp,
            formatter = { s, p -> if (p > 0) "${s.label} ${p}%" else s.label }
        ),
        leaders = PieStyle.Leaders(
            show = true,
            lineWidth = 1.dp,
            offset = 8.dp,
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        PieChart(
            modifier = Modifier.fillMaxSize(),
            data = data,
            style = style
        )
    }
}
