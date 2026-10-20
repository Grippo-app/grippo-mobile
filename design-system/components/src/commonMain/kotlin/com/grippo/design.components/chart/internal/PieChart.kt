package com.grippo.design.components.chart.internal

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.chart.pie.PieChart
import com.grippo.chart.pie.PieData
import com.grippo.chart.pie.PieSlice
import com.grippo.chart.pie.PieStyle
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun PieChart(
    modifier: Modifier = Modifier,
    data: PieData
) {
    val total = data.slices.sumOf { slice ->
        if (slice.value.isFinite() && slice.value > 0f) slice.value.toDouble() else 0.0
    }
    val shareLabels = data.slices.map { slice ->
        val share = if (total > 0.0 && slice.value.isFinite() && slice.value > 0f) {
            (slice.value / total * 100.0).toFloat().coerceIn(0f, 100f)
        } else {
            0f
        }
        slice to PercentageFormatState.shortShare(share)
    }.toMap()
    val style = PieStyle(
        layout = PieStyle.Layout(
            padding = AppTokens.dp.metrics.charts.pie.layoutPadding,
            startAngleDeg = -90f,
        ),
        arc = PieStyle.Arc(
            width = AppTokens.dp.metrics.charts.pie.arcWidth,
            paddingAngleDeg = 2f,
            cornerRadius = AppTokens.dp.metrics.charts.pie.radius,
        ),
        labels = PieStyle.Labels.Outside(
            textStyle = AppTokens.typography.b11Med().copy(color = AppTokens.colors.text.primary),
            labelPadding = AppTokens.dp.metrics.charts.pie.labelPadding,
            formatter = { slice, _ -> "${slice.label} · ${shareLabels[slice]}" }
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
    PreviewContainer {
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
}

@AppPreview
@Composable
private fun PieChartEmptyPreview() {
    PreviewContainer {
        PieChart(
            modifier = Modifier.size(300.dp),
            data = PieData(slices = emptyList()),
        )
    }
}

@AppPreview
@Composable
private fun PieChartTinySharePreview() {
    PreviewContainer {
        PieChart(
            modifier = Modifier.size(300.dp),
            data = PieData(
                slices = listOf(
                    PieSlice("majority", "Majority", 99f, AppTokens.colors.muscle.palette6MuscleCalm.first()),
                    PieSlice("minority", "Minority", 1f, AppTokens.colors.muscle.palette6MuscleCalm.last()),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun PieChartSingleAndZeroPreview() {
    PreviewContainer {
        PieChart(
            modifier = Modifier.size(300.dp),
            data = PieData(
                slices = listOf(PieSlice("single", "One group", 100f, AppTokens.colors.context.muscle)),
            ),
        )
        PieChart(
            modifier = Modifier.size(300.dp),
            data = PieData(
                slices = listOf(PieSlice("zero", "No recorded share", 0f, AppTokens.colors.context.muscle)),
            ),
        )
    }
}

@AppPreview
@Composable
private fun PieChartLongLabelsPreview() {
    PreviewContainer {
        val names = listOf(
            "Quadriceps and lower body",
            "Upper back and trapezius",
            "Chest and anterior deltoids",
            "Arms and forearms",
            "Shoulders and rear deltoids",
            "Abdominals and obliques",
        )
        PieChart(
            modifier = Modifier.size(300.dp),
            data = PieData(
                slices = names.mapIndexed { index, name ->
                    PieSlice(
                        id = index.toString(),
                        label = name,
                        value = if (index == 0) 0.1f else 20f,
                        color = AppTokens.colors.muscle.palette6MuscleCalm[index],
                    )
                },
            ),
        )
    }
}

@AppPreview
@Composable
private fun PieChartDenseLegendPreview() {
    PreviewContainer {
        val colors = AppTokens.colors.muscle.palette6MuscleCalm
        PieChart(
            modifier = Modifier.size(300.dp),
            data = PieData(
                slices = List(18) { index ->
                    PieSlice(
                        id = index.toString(),
                        label = "Recorded muscle group with a complete name ${index + 1}",
                        value = (index + 1).toFloat(),
                        color = colors[index % colors.size],
                    )
                },
            ),
        )
    }
}
