package com.grippo.design.components.chart

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.chart.pie.PieData
import com.grippo.chart.pie.PieSlice
import com.grippo.chart.pie.PieStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview

@Immutable
public data class DSPieSlice(
    val id: String,
    val label: String,
    val value: Float,
    val color: Color
)

@Immutable
public data class DSPieData(
    val slices: List<DSPieSlice>,
    val valueUnit: String? = null,
)

@Composable
public fun PieChart(
    modifier: Modifier = Modifier,
    data: DSPieData
) {
    val style = PieStyle(
        layout = PieStyle.Layout(
            padding = 0.dp,
            startAngleDeg = -90f,
        ),
        arc = PieStyle.Arc(
            width = 18.dp,
            paddingAngleDeg = 2f,
            cornerRadius = 6.dp,
        ),
        labels = PieStyle.Labels.Adaptive(
            insideMinAngleDeg = 14f,
            outsideMinAngleDeg = 5f,
            textStyle = AppTokens.typography.b11Bold().copy(color = AppTokens.colors.text.primary),
            labelPadding = 6.dp,
            formatter = { s, p -> s.label }
        ),
        leaders = PieStyle.Leaders(
            show = true,
            lineWidth = 1.dp,
            offset = 8.dp,
        )
    )

    com.grippo.chart.pie.PieChart(
        modifier = modifier,
        data = remember(data) { data.toChart() },
        style = style
    )
}

private fun DSPieSlice.toChart(): PieSlice = PieSlice(
    id = id,
    label = label,
    value = value,
    color = color
)

private fun DSPieData.toChart(): PieData = PieData(
    slices = slices.map { it.toChart() },
    valueUnit = valueUnit,
)

@AppPreview
@Composable
private fun PieChartPreview() {
    val data = DSPieData(
        slices = listOf(
            DSPieSlice("legs", "Legs", 26f, Color(0xFFFF7A33)),
            DSPieSlice("back", "Back", 18f, Color(0xFF6AA9FF)),
            DSPieSlice("chest", "Chest", 22f, Color(0xFF00E6A7)),
            DSPieSlice("arms", "Arms", 12f, Color(0xFFFFC53D)),
            DSPieSlice("shoulders", "Shoulders", 10f, Color(0xFFB049F8)),
            DSPieSlice("core", "Core", 12f, Color(0xFFFF5E8A)),
        )
    )

    PieChart(
        modifier = Modifier.size(300.dp),
        data = data,
    )
}
