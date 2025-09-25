package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.chart.progress.ProgressChart
import com.grippo.chart.progress.ProgressChartData
import com.grippo.chart.progress.ProgressData
import com.grippo.chart.progress.ProgressStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.ceil

@Composable
internal fun ProgressChart(
    modifier: Modifier = Modifier,
    data: ProgressData
) {
    val charts = AppTokens.colors.charts

    val style = ProgressStyle(
        layout = ProgressStyle.Layout(
            barHeight = 16.dp,
            spacing = 12.dp,
            corner = 10.dp,
            labelPadding = 8.dp,
        ),
        domain = ProgressStyle.Domain.Absolute(
            maxValue = 100f,
        ),
        bars = ProgressStyle.Bars(
            trackColor = charts.progress.track,
            brushProvider = { entry, _, rect ->
                Brush.horizontalGradient(
                    0f to entry.color.copy(alpha = 0.95f),
                    1f to entry.color.copy(alpha = 0.70f),
                    startX = rect.left,
                    endX = rect.right
                )
            },
            strokeWidth = 0.dp,
            strokeColor = AppTokens.colors.divider.default,
        ),
        labels = ProgressStyle.Labels(
            textStyle = AppTokens.typography.b10Med().copy(color = AppTokens.colors.text.primary)
        ),
        values = ProgressStyle.Values.Inside(
            textStyle = AppTokens.typography.b10Bold().copy(color = AppTokens.colors.text.primary),
            formatter = { v, d -> ceil(v).toInt().toString() },
            minInnerPadding = 6.dp,
            insideColor = null,
            preferNormalizedLabels = true,
        ),
        target = null,
        progression = ProgressStyle.Progression.Power(0.5f)
    )

    ProgressChart(
        modifier = modifier,
        data = data,
        style = style
    )
}

@AppPreview
@Composable
private fun ProgressChartPreview() {
    PreviewContainer {
        val ds = ProgressData(
            items = listOf(
                ProgressChartData("Bench Press", 72f, Color(0xFF6AA9FF)),
                ProgressChartData("Deadlift", 100f, Color(0xFF00E6A7)),
                ProgressChartData("Squat", 86f, Color(0xFFFF7A33)),
                ProgressChartData("Overhead Press", 58f, Color(0xFFB049F8)),
                ProgressChartData("Row", 64f, Color(0xFFFFC53D)),
            ),
        )

        ProgressChart(
            modifier = Modifier.size(300.dp),
            data = ds
        )
    }
}