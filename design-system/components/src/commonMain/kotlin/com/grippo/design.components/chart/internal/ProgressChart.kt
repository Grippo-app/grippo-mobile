package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.chart.progress.ProgressChart
import com.grippo.chart.progress.ProgressChartData
import com.grippo.chart.progress.ProgressData
import com.grippo.chart.progress.ProgressStyle
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun ProgressChart(
    modifier: Modifier = Modifier,
    data: ProgressData
) {
    val charts = AppTokens.colors.charts
    val valueLabels = data.items.associate { entry ->
        val label = if (entry.value.isFinite() && entry.value in 0f..100f) {
            PercentageFormatState.shortShare(entry.value)
        } else {
            "—"
        }
        entry.value to label
    }

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
            brushProvider = null,
            strokeWidth = 0.dp,
            strokeColor = AppTokens.colors.divider.default,
        ),
        labels = ProgressStyle.Labels(
            textStyle = AppTokens.typography.b12Semi().copy(color = AppTokens.colors.text.primary)
        ),
        values = ProgressStyle.Values.Outside(
            textStyle = AppTokens.typography.b12Semi().copy(color = AppTokens.colors.text.primary),
            formatter = { value, _ -> valueLabels[value] ?: "—" },
            preferNormalizedLabels = true,
        ),
        target = null,
        progression = ProgressStyle.Progression.Linear,
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

@AppPreview
@Composable
private fun ProgressChartBoundsPreview() {
    PreviewContainer {
        ProgressChart(
            modifier = Modifier.size(300.dp),
            data = ProgressData(
                items = listOf(
                    ProgressChartData("Not started", 0f, AppTokens.colors.muscle.palette6MuscleCalm.first()),
                    ProgressChartData("Small share", 1f, AppTokens.colors.muscle.palette6MuscleCalm.first()),
                    ProgressChartData("Quarter", 25f, AppTokens.colors.muscle.palette6MuscleCalm.first()),
                    ProgressChartData("Complete", 100f, AppTokens.colors.muscle.palette6MuscleCalm.last()),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun ProgressChartEmptyPreview() {
    PreviewContainer {
        ProgressChart(
            modifier = Modifier.size(300.dp),
            data = ProgressData(items = emptyList()),
        )
    }
}

@AppPreview
@Composable
private fun ProgressChartLongLabelsAndTinySharePreview() {
    PreviewContainer {
        ProgressChart(
            modifier = Modifier.size(300.dp),
            data = ProgressData(
                items = listOf(
                    ProgressChartData("Long localized muscle group and supporting movement name", 0.1f, AppTokens.colors.context.muscle),
                    ProgressChartData("Recorded zero", 0f, AppTokens.colors.context.muscle),
                    ProgressChartData("Complete recorded share", 100f, AppTokens.colors.context.muscle),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun ProgressChartUnsupportedValuesPreview() {
    PreviewContainer {
        ProgressChart(
            modifier = Modifier.size(300.dp),
            data = ProgressData(
                items = listOf(
                    ProgressChartData("Unavailable", Float.NaN, AppTokens.colors.context.muscle),
                    ProgressChartData("Infinite value", Float.POSITIVE_INFINITY, AppTokens.colors.context.muscle),
                    ProgressChartData("Outside the share domain", -1f, AppTokens.colors.context.muscle),
                ),
            ),
        )
    }
}
