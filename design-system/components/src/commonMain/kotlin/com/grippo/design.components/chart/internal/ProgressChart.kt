package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
            barHeight = AppTokens.dp.metrics.charts.progress.barHeight,
            spacing = AppTokens.dp.metrics.charts.progress.spacing,
            corner = AppTokens.dp.metrics.charts.progress.radius,
            labelPadding = AppTokens.dp.metrics.charts.progress.labelPadding,
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
            textStyle = AppTokens.typography.b12Semi().copy(color = AppTokens.colors.text.primary)
        ),
        values = ProgressStyle.Values.Inside(
            textStyle = AppTokens.typography.b10Bold().copy(color = AppTokens.colors.static.white),
            formatter = { v, d -> ceil(v).toInt().toString() },
            minInnerPadding = AppTokens.dp.metrics.charts.progress.minInnerPadding,
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
                ProgressChartData("Bench Press", 72f, AppTokens.colors.muscle.palette6MuscleCalm[0]),
                ProgressChartData("Deadlift", 100f, AppTokens.colors.muscle.palette6MuscleCalm[1]),
                ProgressChartData("Squat", 86f, AppTokens.colors.muscle.palette6MuscleCalm[2]),
                ProgressChartData("Overhead Press", 58f, AppTokens.colors.muscle.palette6MuscleCalm[3]),
                ProgressChartData("Row", 64f, AppTokens.colors.muscle.palette6MuscleCalm[4]),
            ),
        )

        ProgressChart(
            modifier = Modifier.size(300.dp),
            data = ds
        )
    }
}
